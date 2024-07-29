package com.jx.blockchain.service.tron;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jx.blockchain.service.tron.crypto.ECKey;
import com.jx.blockchain.service.tron.vo.AccountResourceVo;
import com.jx.blockchain.service.tron.vo.TronTransactionFee;
import com.jx.blockchain.service.tron.vo.TronWallet;
import com.jx.blockchain.vo.JxResponse;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Service;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Bool;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.crypto.SECP256K1;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

import static com.jx.blockchain.service.tron.utils.Base58.decodeFromBase58Check;
import static com.jx.blockchain.service.tron.utils.Base58.encode58Check;
import static org.tron.trident.core.ApiWrapper.calculateTransactionHash;
import static org.tron.trident.core.ApiWrapper.parseAddress;

@Service
public class TronService {
    String apiKey = "9703d76b-dc0b-41e8-a760-1fefb39db240";
    // 主网
    String rpcUrl = "https://api.trongrid.io/jsonrpc";
    ApiWrapper apiWrapper = new ApiWrapper("grpc.trongrid.io:50051",
            "grpc.trongrid.io:50052",
            "", apiKey);
    // 测试网
//    String rpcUrl = "https://nile.trongrid.io/jsonrpc";
//    ApiWrapper apiWrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
//            "grpc.nile.trongrid.io:50061",
//            "", apiKey);

    HttpService tronRpcService = new HttpService(rpcUrl);
    private static final int trxDecimals = 6;

    /**
     * BIP44: m/purpose’/coin’/account’/change/address_index
     * 根据BIP44 tron默认路径为 m/44’/195'/0’/0/0
     */
    private static final ImmutableList<ChildNumber> BIP44_TRON_ACCOUNT_ZERO_PATH =
            ImmutableList.of(new ChildNumber(44, true), new ChildNumber(195, true),
                    ChildNumber.ZERO_HARDENED, ChildNumber.ZERO);

    /**
     * 创建钱包
     */
    public TronWallet createWallet() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
        secureRandom.nextBytes(entropy);
        //生成12位助记词
        List<String> mnemonicList = MnemonicCode.INSTANCE.toMnemonic(entropy);
        //使用助记词生成钱包种子
        byte[] seed = MnemonicCode.toSeed(mnemonicList, "");
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
        System.out.println(BIP44_TRON_ACCOUNT_ZERO_PATH);
        DeterministicKey deterministicKey = deterministicHierarchy
                .deriveChild(BIP44_TRON_ACCOUNT_ZERO_PATH, false, true, new ChildNumber(0));
        byte[] bytes = deterministicKey.getPrivKeyBytes();
        ECKey keyPair = new ECKey(bytes, true);
        //通过公钥生成钱包地址
        String address = encode58Check(keyPair.getAddress());
        String privateKey = Hex.encodeHexString(keyPair.getPrivateKey());
        String publicKey = Hex.encodeHexString(keyPair.getPubKey());
        String mnemonic = Utils.SPACE_JOINER.join(mnemonicList);
        TronWallet wallet = new TronWallet();
        wallet.setAddress(address);
        wallet.setMnemonic(mnemonic);
        wallet.setPrivateKey(privateKey);
        wallet.setPublicKey(publicKey);
        return wallet;
    }

    public String getAddressByPrivateKey(String privateKeyStr) {
        if (privateKeyStr.startsWith("0x")) {
            privateKeyStr = privateKeyStr.substring(2);
        }
        // 使用Web3j的ECKeyPair类来创建一个密钥对
        SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
        KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
        // 使用Web3j的Keys工具类来从密钥对中反推地址
        return keyPair.toBase58CheckAddress();
    }
    /**
     * 查询trx余额
     */
    public JxResponse getBalance(String address) {
        try {
            long balance = apiWrapper.getAccountBalance(address);
            return JxResponse.success(getDecimalBalance(BigInteger.valueOf(balance), trxDecimals));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 查询代币余额
     */
    public JxResponse getTokenBalance(String contractAddress, String fromAddress) {
        try {
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            BigInteger balance = trc20Contract.balanceOf(fromAddress);
            BigInteger decimals = trc20Contract.decimals();
            return JxResponse.success(getDecimalBalance(balance, decimals.intValue()));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 激活账户 createaccount
     */
    public JxResponse createAccount(String ownerAddress, String ownerPrivateKey, String accountAddress) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(ownerAddress);
            // 激活账户需要支付1TRX的账户创建费用
            if (accountBalance < 1000000L) {
                return JxResponse.error(2, "钱包余额不足");
            }
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(ownerPrivateKey, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Response.TransactionExtention transactionExtention = apiWrapper.createAccount(ownerAddress, accountAddress);
            Chain.Transaction transaction = apiWrapper.signTransaction(transactionExtention, keyPair);
            int netNeeded = transaction.toBuilder().clearRet().build().getSerializedSize() + 64;
            JxResponse addressResource = getAddressResource(ownerAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(addressResource.code(), (String) addressResource.data());
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                // 带宽不足使用0.1TRX
                if (accountBalance < 1100000L) {
                    return JxResponse.error(2, "手续费不足");
                }
            }
            String txHash = apiWrapper.broadcastTransaction(transaction);
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取最新区块号
     *
     * @return blockNumber
     */
    public JxResponse getBlockNumber() {
        try {
            Chain.Block nowBlock = apiWrapper.getNowBlock();
            return JxResponse.success(nowBlock.getBlockHeader().getRawData().getNumber());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取最新区块
     *
     * @return Chain.Block
     */
    public JxResponse getNowBlock() {
        try {
            Chain.Block nowBlock = apiWrapper.getNowBlock();
            long latestBlockNumber = nowBlock.getBlockHeader().getRawData().getNumber();
            GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
            builder.setNum(latestBlockNumber);
            Response.BlockExtention block = apiWrapper.blockingStub.getBlockByNum2(builder.build());
            String blockId = Hex.encodeHexString(block.getBlockid().toByteArray());
            System.out.println(blockId);
            return JxResponse.success(block);
        } catch (Exception e) {
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取区块信息
     *
     * @param blockNumber 区块号
     * @return 区块信息
     */
    public JxResponse getBlockByNumber(long blockNumber) {
        // block没有返回区块hash
        try {
            GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
            builder.setNum(blockNumber);
            Response.BlockExtention block = apiWrapper.blockingStub.getBlockByNum2(builder.build());
            Hex.encodeHexString(block.getBlockid().toByteArray());
            return JxResponse.success(block);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }

    }

    /**
     * 根据区块号获取区块hash
     *
     * @param blockNumber 区块号
     * @return hash 区块hash
     */
    public JxResponse getBlockHashByNumber(long blockNumber) {
        try {
            Web3j web3j = Web3j.build(tronRpcService);
            Request<?, EthBlock> request = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), true);
            request.setId(0);
            EthBlock ethBlock = request.send();
            if (ethBlock.hasError()) {
                System.out.println(ethBlock.getError().getMessage());
                return JxResponse.error(1, ethBlock.getError().getMessage());
            }
            String hash = ethBlock.getBlock().getHash();
            return JxResponse.success(hash.substring(2));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取区块交易信息
     *
     * @param blockNumber 区块号
     * @return 区块交易信息
     */
    public JxResponse getTransactionInfoByBlockNum(long blockNumber) {
        try {
            Response.TransactionInfoList transactionInfoByBlockNum = apiWrapper.getTransactionInfoByBlockNum(blockNumber);
            System.out.println(transactionInfoByBlockNum);
            List<Response.TransactionInfo> transactionInfoList = transactionInfoByBlockNum.getTransactionInfoList();
            for (int i = 0; i < transactionInfoList.size(); i++) {
                transactionInfoList.get(i).getContractResult(0);
            }
            return JxResponse.success(transactionInfoByBlockNum);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取交易信息
     *
     * @param txId 交易id
     * @return 交易信息
     */
    public JxResponse getTransactionInfoById(String txId) {
        try {
            Response.TransactionInfo transactionInfo = apiWrapper.getTransactionInfoById(txId);
            return JxResponse.success(transactionInfo);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取交易信息
     *
     * @param txId 交易id
     * @return 交易信息
     */
    public JxResponse getTransactionByIdSolidity(String txId) {
        try {
            Chain.Transaction transaction = apiWrapper.getTransactionByIdSolidity(txId);
            return JxResponse.success(transaction);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取交易信息
     *
     * @param txId 交易id
     * @return 交易信息
     */
    public JxResponse getTransactionById(String txId) {
        try {
            Chain.Transaction transaction = apiWrapper.getTransactionById(txId);
            return JxResponse.success(transaction);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    public <T extends com.google.protobuf.Message> T unpackTransaction(Chain.Transaction transaction, Class<T> clazz) throws InvalidProtocolBufferException {
        return transaction.getRawData().getContract(0).getParameter().unpack(clazz);
    }

    /**
     * 获取交易id
     *
     * @param transaction
     * @return 交易id
     */
    public String getIdByTransaction(Chain.Transaction transaction) {
        byte[] bytes = calculateTransactionHash(transaction);
        return ByteString.copyFrom(org.bouncycastle.util.encoders.Hex.encode(bytes)).toStringUtf8();
    }

    /**
     * 获取交易手续费信息
     *
     * @param txId 交易id
     * @return 交易手续费信息
     */
    public JxResponse getTransactionFeeById(String txId) {
        try {
            Response.TransactionInfo transactionInfo = apiWrapper.getTransactionInfoById(txId);
            Response.ResourceReceipt receipt = transactionInfo.getReceipt();
            System.out.println("receipt:" + receipt);
            TronTransactionFee transactionFee = new TronTransactionFee();
            transactionFee.setFee(transactionInfo.getFee());
            transactionFee.setNetFee(receipt.getNetFee());
            transactionFee.setNetUsage(receipt.getNetUsage());
            transactionFee.setFee(receipt.getEnergyFee());
            transactionFee.setEnergyUsage(receipt.getEnergyUsage());
            transactionFee.setEnergyUsageTotal(receipt.getEnergyUsageTotal());
            transactionFee.setOriginEnergyUsage(receipt.getOriginEnergyUsage());
            return JxResponse.success(transactionFee);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取交易手续费信息
     *
     * @param transactionInfo
     * @return 交易手续费信息
     */
    public JxResponse getFeeByTransactionInfo(Response.TransactionInfo transactionInfo) {
        try {
            Response.ResourceReceipt receipt = transactionInfo.getReceipt();
            System.out.println("receipt:" + receipt);
            TronTransactionFee transactionFee = new TronTransactionFee();
            transactionFee.setFee(transactionInfo.getFee());
            transactionFee.setNetFee(receipt.getNetFee());
            transactionFee.setNetUsage(receipt.getNetUsage());
            transactionFee.setFee(receipt.getEnergyFee());
            transactionFee.setEnergyUsage(receipt.getEnergyUsage());
            transactionFee.setEnergyUsageTotal(receipt.getEnergyUsageTotal());
            transactionFee.setOriginEnergyUsage(receipt.getOriginEnergyUsage());
            return JxResponse.success(transactionFee);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取能量单价
     *
     * @return 1能量多少sun
     */
    public JxResponse getEnergyPrice() {
        try {
            Web3j web3j = Web3j.build(tronRpcService);
            Request<?, EthGasPrice> request = web3j.ethGasPrice();
            request.setId(0);
            EthGasPrice ethGasPrice = request.send();
            if (ethGasPrice.hasError()) {
                System.out.println(ethGasPrice.getError().getMessage());
                return JxResponse.error(1, ethGasPrice.getError().getMessage());
            }
            return JxResponse.success(ethGasPrice.getGasPrice());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取Trx转账预计消耗带宽
     */
    public JxResponse estimateTrxBandwidthUsed(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount) {
        try {
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.create(privateKey);
            Response.TransactionExtention transactionExtention = apiWrapper.transfer(fromAddress, toAddress, getBigIntegerBalance(amount, trxDecimals).longValue());
            Chain.Transaction signedTransaction = apiWrapper.signTransaction(transactionExtention, new KeyPair(keyPair));
            return JxResponse.success(signedTransaction.toBuilder().clearRet().build().getSerializedSize() + 64);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取Trc20转账预计消耗带宽
     */
    public JxResponse estimateTrc20BandwidthUsed(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount, String contractAddress) {
        try {
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            Function transfer = new Function("transfer",
                    Arrays.asList(new Address(toAddress),
                            new Uint256(amount.multiply(BigDecimal.TEN.pow(trc20Contract.decimals().intValue())).toBigInteger())),
                    Arrays.asList(new TypeReference<Bool>() {
                    }));
            TransactionBuilder builder = apiWrapper.triggerCall(Base58Check.bytesToBase58(parseAddress(fromAddress).toByteArray()),
                    Base58Check.bytesToBase58(trc20Contract.getCntrAddr().toByteArray()), transfer);
            //feeLimit仅指调用者愿意承担的Energy折合的trx；执行合约允许的最大Energy还包括开发者承担的部分；
            // 单位sun  默认50Trx
            builder.setFeeLimit(50000000);
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Chain.Transaction signedTxn = apiWrapper.signTransaction(builder.build(), keyPair);
            return JxResponse.success(apiWrapper.estimateBandwidth(signedTxn));
//            return JxResponse.success(signedTxn.toBuilder().clearRet().build().getSerializedSize() + 64);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取TRC20交易预计所需能量
     *
     * @return 交易所需能量
     */
    public JxResponse estimateEnergyUsed(String fromAddress, String toAddress, BigDecimal amount, String contractAddress) {
        try {
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            // 构建方法调用信息
            String method = "transfer";
            // 构建输入参数
            List<Type> inputArgs = new ArrayList<>();
            org.web3j.abi.datatypes.Address fAddress = new org.web3j.abi.datatypes.Address(168, formatAddressAsRpc(toAddress));
            String fContractAddress = formatAddressAsRpc(contractAddress, true);
            inputArgs.add(fAddress);
            BigInteger bigAmount = amount.multiply(BigDecimal.TEN.pow(trc20Contract.decimals().intValue())).toBigInteger();
            inputArgs.add(new org.web3j.abi.datatypes.generated.Uint256(bigAmount));
            // 合约返回值容器
            List<org.web3j.abi.TypeReference<?>> outputArgs = new ArrayList<>();
            String funcABI = FunctionEncoder.encode(new org.web3j.abi.datatypes.Function(method, inputArgs, outputArgs));
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    formatAddressAsRpc(fromAddress), null, null, null, fContractAddress, funcABI);
            Web3j web3j = Web3j.build(tronRpcService);
            Request<?, EthEstimateGas> request = web3j.ethEstimateGas(transaction);
            request.setId(0);
            EthEstimateGas ethEstimateGas = request.send();
            if (ethEstimateGas.hasError()) {
                System.out.println(ethEstimateGas.getError().getMessage());
                return JxResponse.error(1, ethEstimateGas.getError().getMessage());
            }
            return JxResponse.success(ethEstimateGas.getAmountUsed());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }


    /**
     * trx转账
     *
     * @param privateKeyStr 转出地址私钥
     * @param toAddress        收帐地址
     * @param amount           转帐数量
     * @return 转账结果
     */
    public JxResponse transferTrx(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(fromAddress);
            long fee = 0;
            if (getDecimalBalance(BigInteger.valueOf(accountBalance), trxDecimals).compareTo(amount) < 0) {
                return JxResponse.error(2, "钱包余额不足");
            }
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Response.TransactionExtention transactionExtention = apiWrapper.transfer(fromAddress, toAddress, amount.multiply(BigDecimal.TEN.pow(6)).longValue());
            Chain.Transaction transaction = apiWrapper.signTransaction(transactionExtention, keyPair);
            int netNeeded = transaction.toBuilder().clearRet().build().getSerializedSize() + 64;
            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(addressResource.code(), (String) addressResource.data());
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                // 带宽单价为1000sun
                fee += netNeeded * 1000L;
            }
            if (accountBalance < (fee + getBigIntegerBalance(amount, trxDecimals).longValue())) {
                return JxResponse.error(2, "手续费不足");
            }
            String txHash = apiWrapper.broadcastTransaction(transaction);
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * trx转账
     * 手续费不足时 从转账金额中扣除
     *
     * @param privateKeyStr 转出地址私钥
     * @param toAddress        收帐地址
     * @param amount           转帐数量
     * @return 转账结果
     */
    public JxResponse transferTrxWithFee(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(fromAddress);
            long fee = 0;
            if (getDecimalBalance(BigInteger.valueOf(accountBalance), trxDecimals).compareTo(amount) < 0) {
                return JxResponse.error(2, "钱包余额不足");
            }
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Response.TransactionExtention transactionExtention = apiWrapper.transfer(fromAddress, toAddress, amount.multiply(BigDecimal.TEN.pow(6)).longValue());
            Chain.Transaction transaction = apiWrapper.signTransaction(transactionExtention, keyPair);
            int netNeeded = transaction.toBuilder().clearRet().build().getSerializedSize() + 64;
            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(addressResource.code(), (String) addressResource.data());
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                // 带宽单价为1000sun
                fee += netNeeded * 1000L;
            }
            if (accountBalance < (fee + getBigIntegerBalance(amount, trxDecimals).longValue())) {
                amount = BigDecimal.valueOf(accountBalance - fee);
            }
            JxResponse JxResponse = transferTrx(fromAddress, privateKeyStr, toAddress, amount);
            if (JxResponse.code() != 0) {
                return JxResponse.error(JxResponse.code(), (String) JxResponse.data());
            }
            String txHash = (String) JxResponse.data();
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }


    /**
     * trx转账 带宽不足不发起交易
     *
     * @param privateKeyStr 转出地址私钥
     * @param toAddress        收帐地址
     * @param amount           转帐数量
     * @return 转账结果
     */
    public JxResponse transferTrxWithResource(String fromAddress, String privateKeyStr, String toAddress
            , BigDecimal amount) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(fromAddress);
            if (getDecimalBalance(BigInteger.valueOf(accountBalance), trxDecimals).compareTo(amount) < 0) {
                return JxResponse.error(2, "钱包余额不足");
            }
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Response.TransactionExtention transactionExtention = apiWrapper.transfer(fromAddress, toAddress, amount.multiply(BigDecimal.TEN.pow(6)).longValue());
            Chain.Transaction transaction = apiWrapper.signTransaction(transactionExtention, keyPair);
            int netNeeded = transaction.toBuilder().clearRet().build().getSerializedSize() + 64;
            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(addressResource.code(), (String) addressResource.data());
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                return JxResponse.error(4, "带宽不足,剩余带宽:[" + remainNet + "],需要带宽:[" + netNeeded + "]");
            }
            JxResponse JxResponse = transferTrx(fromAddress, privateKeyStr, toAddress, amount);
            if (JxResponse.code() != 0) {
                return JxResponse.error(JxResponse.code(), (String) JxResponse.data());
            }
            String txHash = (String) JxResponse.data();
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * trx20转账 资源不足燃烧TRX
     *
     * @param privateKeyStr 转出地址私钥
     * @param toAddress        收帐地址
     * @param amount           转帐数量
     * @param contractAddress  trx20合约地址
     * @return 转账结果
     */
    public JxResponse transferTrc20(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount, String contractAddress) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(fromAddress);
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            BigInteger tokenBalance = trc20Contract.balanceOf(fromAddress);
            BigInteger decimals = trc20Contract.decimals();
            if (tokenBalance.compareTo(getBigIntegerBalance(amount, decimals.intValue())) < 0) {
                return JxResponse.error(1, "代币余额不足");
            }
            Function transfer = new Function("transfer",
                    Arrays.asList(new Address(toAddress),
                            new Uint256(amount.multiply(BigDecimal.TEN.pow(decimals.intValue())).toBigInteger())),
                    Arrays.asList(new TypeReference<Bool>() {
                    }));
            TransactionBuilder builder = apiWrapper.triggerCall(Base58Check.bytesToBase58(parseAddress(fromAddress).toByteArray()),
                    Base58Check.bytesToBase58(trc20Contract.getCntrAddr().toByteArray()), transfer);
            // feeLimit仅指调用者愿意承担的Energy折合的trx；执行合约允许的最大Energy还包括开发者承担的部分；
            // 默认50Trx(50000000sun)
            builder.setFeeLimit(50000000);
            Chain.Transaction signedTxn = apiWrapper.signTransaction(builder.build(), keyPair);
            long fee = 0;
            JxResponse estimateEnergyUsedResult = estimateEnergyUsed(fromAddress, toAddress, amount, contractAddress);
            if (estimateEnergyUsedResult.code() != 0) {
                return JxResponse.error(3, "预估能量消耗失败");
            }
            int netNeeded = signedTxn.toBuilder().clearRet().build().getSerializedSize() + 64;
            BigInteger estimateEnergyUsed = (BigInteger) estimateEnergyUsedResult.data();

            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(3, "获取地址资源失败");
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                // 带宽单价为1000sun
                fee += netNeeded * 1000L;
            }
            long remainEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
            if (remainEnergy < estimateEnergyUsed.longValue()) {
                JxResponse energyPriceResult = getEnergyPrice();
                if (energyPriceResult.code() != 0) {
                    return JxResponse.error(3, "获取能量单价失败");
                }
                BigInteger energyPrice = (BigInteger) energyPriceResult.data();
                long burnTrx = energyPrice.longValue() * (estimateEnergyUsed.longValue() - remainEnergy);
                fee += burnTrx;
            }
            // 账户余额小于交易需要燃烧的trx 或者交易需要燃烧的trx大于feeLimit(20000000sun)
            if (accountBalance < fee || fee > 20000000) {
                return JxResponse.error(4, "手续费不足");
            }
            String txHash = apiWrapper.broadcastTransaction(signedTxn);
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * trx20转账 资源不足则失败
     *
     * @param privateKeyStr 转出地址私钥
     * @param toAddress        收帐地址
     * @param amount           转帐数量
     * @param contractAddress  trx20合约地址
     * @return 转账结果
     */
    public JxResponse transferTrc20WithResource(String fromAddress, String privateKeyStr, String toAddress, BigDecimal amount, String contractAddress) {
        try {
            long accountBalance = apiWrapper.getAccountBalance(fromAddress);
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger(privateKeyStr, 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            BigInteger tokenBalance = trc20Contract.balanceOf(fromAddress);
            BigInteger decimals = trc20Contract.decimals();
            if (tokenBalance.compareTo(getBigIntegerBalance(amount, decimals.intValue())) < 0) {
                return JxResponse.error(1, "代币余额不足");
            }
            Function transfer = new Function("transfer",
                    Arrays.asList(new Address(toAddress),
                            new Uint256(amount.multiply(BigDecimal.TEN.pow(decimals.intValue())).toBigInteger())),
                    Arrays.asList(new TypeReference<Bool>() {
                    }));
            TransactionBuilder builder = apiWrapper.triggerCall(Base58Check.bytesToBase58(parseAddress(fromAddress).toByteArray()),
                    Base58Check.bytesToBase58(trc20Contract.getCntrAddr().toByteArray()), transfer);
            // feeLimit仅指调用者愿意承担的Energy折合的trx；执行合约允许的最大Energy还包括开发者承担的部分；
            // 默认50Trx(50000000sun)
            builder.setFeeLimit(50000000);
            Chain.Transaction signedTxn = apiWrapper.signTransaction(builder.build(), keyPair);
            JxResponse estimateEnergyUsedResult = estimateEnergyUsed(fromAddress, toAddress, amount, contractAddress);
            if (estimateEnergyUsedResult.code() != 0) {
                return JxResponse.error(3, "预估能量消耗失败");
            }
            int netNeeded = signedTxn.toBuilder().clearRet().build().getSerializedSize() + 64;
            BigInteger estimateEnergyUsed = (BigInteger) estimateEnergyUsedResult.data();

            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(3, "获取地址资源失败");
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                return JxResponse.error(4, "带宽不足,剩余带宽:[" + remainNet + "],需要带宽:[" + netNeeded + "]");
            }
            long remainEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
            if (remainEnergy < estimateEnergyUsed.longValue()) {
                return JxResponse.error(4, "能量不足,剩余能量:[" + remainEnergy + "],需要能量:[" + estimateEnergyUsed + "]");
            }
            String txHash = apiWrapper.broadcastTransaction(signedTxn);
            return JxResponse.success(txHash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    // 获取交易需要燃烧的trx
    public JxResponse getTransferBurnTrx(String fromAddress, String toAddress, BigDecimal amount, String contractAddress) {
        if (StringUtils.isNotBlank(contractAddress)) {
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract trc20Contract = new Trc20Contract(contract, fromAddress, apiWrapper);
            BigInteger tokenBalance = trc20Contract.balanceOf(fromAddress);
            BigInteger decimals = trc20Contract.decimals();
            if (tokenBalance.compareTo(new BigInteger(String.valueOf(amount))) < 0) {
                return JxResponse.error(1, "代币余额不足");
            }
            Function transfer = new Function("transfer",
                    Arrays.asList(new Address(toAddress),
                            new Uint256(amount.toBigInteger())),
                    Arrays.asList(new TypeReference<Bool>() {
                    }));
            TransactionBuilder builder = apiWrapper.triggerCall(Base58Check.bytesToBase58(parseAddress(fromAddress).toByteArray()),
                    Base58Check.bytesToBase58(trc20Contract.getCntrAddr().toByteArray()), transfer);
            // feeLimit仅指调用者愿意承担的Energy折合的trx；执行合约允许的最大Energy还包括开发者承担的部分；
            // 默认50Trx(50000000sun)
            builder.setFeeLimit(50000000);
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger("d6a74117c0c406e974d99ccbe4502dce273edf5c9ca16f60642d0f9d511f0e86", 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Chain.Transaction signedTxn = apiWrapper.signTransaction(builder.build(), keyPair);
            long fee = 0;
            JxResponse estimateEnergyUsedResult = estimateEnergyUsed(fromAddress, toAddress,
                    amount.divide(BigDecimal.TEN.pow(decimals.intValue()), decimals.intValue(), RoundingMode.UP), contractAddress);
            if (estimateEnergyUsedResult.code() != 0) {
                return JxResponse.error(3, "预估能量消耗失败");
            }
            int netNeeded = signedTxn.toBuilder().clearRet().build().getSerializedSize() + 64;
            BigInteger estimateEnergyUsed = (BigInteger) estimateEnergyUsedResult.data();

            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(3, "获取地址资源失败");
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet < netNeeded) {
                // 带宽单价为1000sun
                fee += netNeeded * 1000L;
            }
            long remainEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
            if (remainEnergy < estimateEnergyUsed.longValue()) {
                JxResponse energyPriceResult = getEnergyPrice();
                if (energyPriceResult.code() != 0) {
                    return JxResponse.error(3, "获取能量单价失败");
                }
                BigInteger energyPrice = (BigInteger) energyPriceResult.data();
                long burnTrx = energyPrice.longValue() * (estimateEnergyUsed.longValue() - remainEnergy);
                fee += burnTrx;
            }
            return JxResponse.success(getDecimalBalance(new BigInteger(String.valueOf(fee)), trxDecimals));
        } else {
            Response.TransactionExtention transactionExtention = null;
            try {
                transactionExtention = apiWrapper.transfer(fromAddress, toAddress, amount.longValue());
            } catch (IllegalException e) {
                return JxResponse.error(3, "构建交易失败");
            }
            // 为精确得出带框消耗, 模拟签名
            SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(new BigInteger("d6a74117c0c406e974d99ccbe4502dce273edf5c9ca16f60642d0f9d511f0e86", 16));
            KeyPair keyPair = new KeyPair(SECP256K1.KeyPair.create(privateKey));
            Chain.Transaction transaction = apiWrapper.signTransaction(transactionExtention, keyPair);
            int netNeeded = transaction.toBuilder().clearRet().build().getSerializedSize() + 64;
            JxResponse addressResource = getAddressResource(fromAddress);
            if (addressResource.code() != 0) {
                return JxResponse.error(addressResource.code(), (String) addressResource.data());
            }
            Response.AccountResourceMessage accountResource = (Response.AccountResourceMessage) addressResource.data();
            long addressNet = accountResource.getNetLimit() + accountResource.getFreeNetLimit();
            long addressNetUsed = accountResource.getNetUsed() + accountResource.getFreeNetUsed();
            long remainNet = addressNet - addressNetUsed;
            if (remainNet > netNeeded) {
                return JxResponse.success(0);
            } else {
                return JxResponse.success(getDecimalBalance(new BigInteger(String.valueOf(netNeeded * 1000)), trxDecimals));
            }
        }
    }

    /**
     * 获取地址资源
     */
    public JxResponse getAddressResource(String address) {
        try {
            Response.AccountResourceMessage accountResource = apiWrapper.getAccountResource(address);
            System.out.println(accountResource);
            return JxResponse.success(accountResource);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }


    /**
     * 获取地址资源
     */
    public JxResponse getAddressResourceForSummarize(String address) {
        try {
            Response.AccountResourceMessage accountResource = apiWrapper.getAccountResource(address);
            AccountResourceVo accountResourceVo = new AccountResourceVo();

            accountResourceVo.setEnergyLimit(BigDecimal.valueOf(accountResource.getEnergyLimit()));
            accountResourceVo.setEnergyUsed(BigDecimal.valueOf(accountResource.getEnergyUsed()));
            accountResourceVo.setFreeNetLimit(BigDecimal.valueOf(accountResource.getFreeNetLimit()));
            accountResourceVo.setNetLimit(BigDecimal.valueOf(accountResource.getNetLimit()));
            accountResourceVo.setNetUsed(BigDecimal.valueOf(accountResource.getNetUsed()));
            accountResourceVo.setFreeNetUsed(BigDecimal.valueOf(accountResource.getFreeNetUsed()));

            accountResourceVo.setTotalEnergyLimit(BigDecimal.valueOf(accountResource.getTotalEnergyLimit()));
            accountResourceVo.setTotalEnergyWeight(BigDecimal.valueOf(accountResource.getTotalEnergyWeight()));
            accountResourceVo.setTotalNetLimit(BigDecimal.valueOf(accountResource.getTotalNetLimit()));
            accountResourceVo.setTotalNetWeight(BigDecimal.valueOf(accountResource.getTotalNetWeight()));

            return JxResponse.success(accountResourceVo);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    /**
     * 获取地址资源 资源类型，0为带宽，1为能量
     */
    public JxResponse getCanDelegatedMaxSize(String address, int type) {
        try {
            long maxSize = apiWrapper.getCanDelegatedMaxSize(address, type);
            return JxResponse.success(maxSize);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }


    /**
     * 将tron地址转化为rpc调用所需的地址格式
     *
     * @param address tron地址
     * @return rpc格式地址
     */
    public static String formatAddressAsRpc(String address, boolean... isContract) {
        byte[] addressBytes = decodeFromBase58Check(address);
        assert addressBytes != null;
        return Hex.encodeHexString(addressBytes);
    }

    public BigInteger getTokenDecimals(String contractAddress) {
        Contract contract = apiWrapper.getContract(contractAddress);
        Trc20Contract trc20Contract = new Trc20Contract(contract, contractAddress, apiWrapper);
        return trc20Contract.decimals();
    }

    public static BigDecimal getDecimalBalance(BigInteger balance, int decimals) {
        BigDecimal decimalBalance = new BigDecimal(balance);
        decimalBalance = decimalBalance.divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.HALF_UP);
        return decimalBalance;
    }

    public static BigInteger getBigIntegerBalance(BigDecimal balance, int decimals) {
        return balance.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger();
    }


}
