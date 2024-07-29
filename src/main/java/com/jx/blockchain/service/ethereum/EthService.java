package com.jx.blockchain.service.ethereum;

import com.google.common.collect.ImmutableList;
import com.jx.blockchain.vo.JxResponse;
import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class EthService {
    // 主网
    // private final static Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/"));
//    private final int chainId = 1;

    // holesky测试网
    private final static Web3j web3j = Web3j.build(new HttpService("https://endpoints.omniatech.io/v1/eth/holesky/public"));
    private static final int chainId = 17000;

    private static final String emptyAddress = "0x0000000000000000000000000000000000000000";

    public static boolean isValidAddress(String input) {
        String cleanInput = Numeric.cleanHexPrefix(input);
        try {
            Numeric.toBigIntNoPrefix(cleanInput);
        } catch (NumberFormatException e) {
            return false;
        }
        return cleanInput.length() == 40;
    }

    /**
     * 创建钱包
     */
    public static String createWallet() throws Exception {

        ImmutableList<ChildNumber> BIP44_ETH_ACCOUNT_ZERO_PATH =
                ImmutableList.of(new ChildNumber(44, true), new ChildNumber(60, true),
                        ChildNumber.ZERO_HARDENED, ChildNumber.ZERO);

        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
        secureRandom.nextBytes(entropy);
        //生成12位助记词
        List<String> mnemonicList = MnemonicCode.INSTANCE.toMnemonic(entropy);
        //使用助记词生成钱包种子
        byte[] seed = MnemonicCode.toSeed(mnemonicList, "");
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
        DeterministicKey deterministicKey = deterministicHierarchy
                .deriveChild(BIP44_ETH_ACCOUNT_ZERO_PATH, false, true, new ChildNumber(0));
        byte[] bytes = deterministicKey.getPrivKeyBytes();
        ECKeyPair keyPair = ECKeyPair.create(bytes);
        //通过公钥生成钱包地址
        String address = "0x" + Keys.getAddress(keyPair.getPublicKey());
        String privateKey = "0x" + keyPair.getPrivateKey().toString(16);
        System.out.println(String.join(" ", mnemonicList));
        System.out.println(privateKey);
        return address;
    }

    public static String getAddressByPrivateKey(String privateKey) {
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        // 使用Web3j的ECKeyPair类来创建一个密钥对
        ECKeyPair keyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        // 使用Web3j的Keys工具类来从密钥对中反推地址
        return Keys.getAddress(keyPair.getPublicKey());
    }

    // 获取ETH BlockNumber
    public static JxResponse getBlockNumber() {
        try {
            Request<?, EthBlockNumber> request = web3j.ethBlockNumber();
            request.setId(0);
            EthBlockNumber ethBlockNumber = request.send();
            if (ethBlockNumber.hasError()) {
                System.out.println(ethBlockNumber.getError().getMessage());
                return JxResponse.error(1, ethBlockNumber.getError().getMessage());
            }
            return JxResponse.success(ethBlockNumber.getBlockNumber());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 获取账户的Nonce
    public static JxResponse getNonce(String address) {
        try {
            Request<?, EthGetTransactionCount> request = web3j.ethGetTransactionCount(
                    address, DefaultBlockParameterName.PENDING);
            request.setId(0);
            EthGetTransactionCount ethGetTransactionCount = request.send();
            if (ethGetTransactionCount.hasError()) {
                System.out.println(ethGetTransactionCount.getError().getMessage());
                return JxResponse.error(1, ethGetTransactionCount.getError().getMessage());
            }
            return JxResponse.success(ethGetTransactionCount.getTransactionCount());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 获取区块信息
    public static JxResponse getBlockByNumber(BigInteger blockNumber) {
        try {
            Request<?, EthBlock> request = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true);
            request.setId(0);
            EthBlock ethBlock = request.send();
            if (ethBlock.hasError()) {
                System.out.println(ethBlock.getError().getMessage());
                return JxResponse.error(1, ethBlock.getError().getMessage());
            }
            return JxResponse.success(ethBlock.getResult());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 获取ETH余额
    public static JxResponse getEthBalance(String address) {
        try {
            Request<?, EthGetBalance> request = web3j.ethGetBalance(address, DefaultBlockParameterName.PENDING);
            request.setId(0);
            EthGetBalance ethGetBalance = request.send();
            if (ethGetBalance.hasError()) {
                System.out.println(ethGetBalance.getError().getMessage());
                return JxResponse.error(1, ethGetBalance.getError().getMessage());
            }
            return JxResponse.success(ethGetBalance.getBalance());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 获取gas price
    public static JxResponse getGasPrice() {
        try {
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
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 估算手续费上限
    public static JxResponse getEthEstimateGas(org.web3j.protocol.core.methods.request.Transaction
                                                       transaction) {
        try {
            Request<?, EthEstimateGas> request = web3j.ethEstimateGas(transaction);
            request.setId(0);
            EthEstimateGas ethEstimateGas = request.send();
            if (ethEstimateGas.hasError()) {
                System.out.println(ethEstimateGas.getError().getMessage());
                return JxResponse.error(1, ethEstimateGas.getError().getMessage());
            }
            return JxResponse.success(ethEstimateGas.getAmountUsed());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 查询以及坊Transactions详情
    public static JxResponse getEthTransaction(String txHash) {
        try {
            Request<?, EthTransaction> request = web3j.ethGetTransactionByHash(txHash);
            request.setId(0);
            EthTransaction ethTransaction = request.send();
            if (ethTransaction.hasError()) {
                return JxResponse.error(1, ethTransaction.getError().getMessage());
            }
            return JxResponse.success(ethTransaction.getResult());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    public static JxResponse getEthTransactionReceipt(String txHash) {
        try {
            Request<?, EthGetTransactionReceipt> request = web3j.ethGetTransactionReceipt(txHash);
            request.setId(0);
            EthGetTransactionReceipt transactionReceipt = request.send();
            if (transactionReceipt.hasError()) {
                return JxResponse.error(1, transactionReceipt.getError().getMessage());
            }
            return JxResponse.success(transactionReceipt.getResult());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 转账ETH 使用私钥
    public static JxResponse transferEth(String fromAddress, String toAddress,
                                         String privateKey, BigDecimal amount) {
        try {
            // 获取fastGasPrice
            JxResponse resGasPrice = getGasPrice();
            if (resGasPrice.code() != 0) return resGasPrice;
            BigInteger fastGasPrice = (BigInteger) resGasPrice.data();
            // 获得nonce
            JxResponse nonceRes = getNonce(fromAddress);
            if (nonceRes.code() != 0) return nonceRes;
            BigInteger nonce = (BigInteger) nonceRes.data();
            // value 转换
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            // transaction
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                    fromAddress, null, null, null, toAddress, null);
            // 计算gasLimit
            JxResponse gasLimitRes = getEthEstimateGas(transaction);
            if (gasLimitRes.code() != 0) return gasLimitRes;
            BigInteger gasLimit = (BigInteger) gasLimitRes.data();
            BigInteger gasMaxUsed = fastGasPrice.multiply(gasLimit);
            // 查询调用者余额，检测余额是否充足
            JxResponse ethBalanceRes = getEthBalance(fromAddress);
            if (ethBalanceRes.code() != 0) return ethBalanceRes;
            BigInteger ethBalance = (BigInteger) ethBalanceRes.data();
            System.out.println("ethBalance = " + Convert.fromWei(new BigDecimal(ethBalance), Convert.Unit.ETHER)
                    + " value = " + Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER));
            if (ethBalance.compareTo(value) < 0) {
                return JxResponse.error(2, "以太坊余额不足，请核实");
            }
            if (ethBalance.compareTo(value.add(gasMaxUsed)) < 0) {
                return JxResponse.error(3, "手续费不足，请核实");
            }
            // 获取私钥信息
            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);
            // 构建交易
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, fastGasPrice, gasLimit, toAddress, value);
            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println("ethSendTransaction Error: " + ethSendTransaction.getError().getMessage());
                return JxResponse.error(3, ethSendTransaction.getError().getMessage());
            } else {
                return JxResponse.success(ethSendTransaction);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 指定nonce转账ETH 使用私钥
    public static JxResponse transferEthByNonce(String fromAddress, String toAddress,
                                                String privateKey, BigDecimal amount,
                                                BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice
    ) {
        try {
            BigInteger gasMaxUsed = gasPrice.multiply(gasLimit);
            // 查询调用者余额，检测余额是否充足
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            JxResponse ethBalanceRes = getEthBalance(fromAddress);
            if (ethBalanceRes.code() != 0) return ethBalanceRes;
            BigInteger ethBalance = (BigInteger) ethBalanceRes.data();
            System.out.println("ethBalance = " + Convert.fromWei(new BigDecimal(ethBalance), Convert.Unit.ETHER)
                    + " value = " + Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER));
            if (ethBalance.add(value).compareTo(value) < 0) {
                return JxResponse.error(2, "以太坊余额不足，请核实");
            }
            if (ethBalance.compareTo(gasMaxUsed) < 0) {
                return JxResponse.error(3, "手续费不足，请核实");
            }
            // 获取私钥信息
            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);
            // 构建交易
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, gasPrice, gasLimit, toAddress, value);
            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println("ethSendTransaction Error: " + ethSendTransaction.getError().getMessage());
                return JxResponse.error(3, ethSendTransaction.getError().getMessage());
            } else {
                return JxResponse.success(ethSendTransaction);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 转账ETH 手续费不足时尝试从交易金额中扣除手续费
    public static JxResponse transferEthWithFee(String fromAddress, String toAddress,
                                                String privateKey, BigDecimal amount) {
        try {
            // 获取fastGasPrice
            JxResponse resGasPrice = getGasPrice();
            if (resGasPrice.code() != 0) return resGasPrice;
            BigInteger fastGasPrice = (BigInteger) resGasPrice.data();
            // 获得nonce
            JxResponse nonceRes = getNonce(fromAddress);
            if (nonceRes.code() != 0) return nonceRes;
            BigInteger nonce = (BigInteger) nonceRes.data();
            // value 转换
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            // transaction
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                    fromAddress, null, null, null, toAddress, null);
            // 计算gasLimit
            JxResponse gasLimitRes = getEthEstimateGas(transaction);
            if (gasLimitRes.code() != 0) return gasLimitRes;
            BigInteger gasLimit = (BigInteger) gasLimitRes.data();
            BigInteger gasMaxUsed = fastGasPrice.multiply(gasLimit);
            // 查询调用者余额，检测余额是否充足
            JxResponse ethBalanceRes = getEthBalance(fromAddress);
            if (ethBalanceRes.code() != 0) return ethBalanceRes;
            BigInteger ethBalance = (BigInteger) ethBalanceRes.data();
            System.out.println("ethBalance = " + Convert.fromWei(new BigDecimal(ethBalance), Convert.Unit.ETHER)
                    + " value = " + Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER));
            if (ethBalance.compareTo(value) < 0) {
                return JxResponse.error(2, "以太坊余额不足，请核实");
            }
            if (ethBalance.compareTo(value.add(gasMaxUsed)) < 0) {
                // 手续费不足时尝试从交易金额中扣除手续费
                value = value.subtract(gasMaxUsed);
                if (value.compareTo(BigInteger.ZERO) < 0) {
                    return JxResponse.error(3, "手续费不足，请核实");
                }
            }
            // 获取私钥信息
            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);
            // 构建交易
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, fastGasPrice, gasLimit, toAddress, value);
            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println("ethSendTransaction Error: " + ethSendTransaction.getError().getMessage());
                return JxResponse.error(3, ethSendTransaction.getError().getMessage());
            } else {
                return JxResponse.success(ethSendTransaction);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 转账代币
    public static JxResponse transferToken(String fromAddress, String toAddress,
                                           String privateKey, String contractAddress, BigDecimal amount) {
        try {
            // 获取fastGasPrice
            JxResponse resGasPrice = getGasPrice();
            if (resGasPrice.code() != 0) return resGasPrice;
            BigInteger fastGasPrice = (BigInteger) resGasPrice.data();
            // 获得nonce
            JxResponse nonceRes = getNonce(fromAddress);
            if (nonceRes.code() != 0) return nonceRes;
            BigInteger nonce = (BigInteger) nonceRes.data();
            // 构建方法调用信息
            String method = "transfer";
            // amount 转换为整数
            JxResponse decimalsRes = getTokenDecimals(contractAddress);
            if (decimalsRes.code() != 0) return decimalsRes;
            int decimals = (int) decimalsRes.data();
            BigInteger bigAmount = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger();
            JxResponse tokenBalanceRes = getTokenBalance(fromAddress, contractAddress);
            if (tokenBalanceRes.code() != 0) return tokenBalanceRes;
            BigInteger tokenBalance = (BigInteger) tokenBalanceRes.data();
            System.out.println("tokenBalance = " + tokenBalance + " bigAmount = " + bigAmount);
            if (tokenBalance.compareTo(bigAmount) < 0) {
                return JxResponse.error(2, "代币余额不足，请核实");
            }

            // 构建输入参数
            List<Type> inputArgs = new ArrayList<>();
            inputArgs.add(new Address(toAddress));
            inputArgs.add(new Uint256(bigAmount));
            // 合约返回值容器
            List<TypeReference<?>> outputArgs = new ArrayList<>();
            String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                    fromAddress, null, null, null, contractAddress, funcABI);
            JxResponse gasLimitRes = getEthEstimateGas(transaction);
            if (gasLimitRes.code() != 0) return gasLimitRes;
            BigInteger gasLimit = (BigInteger) gasLimitRes.data();
            System.out.println("gasLimit = " + gasLimit);
            BigInteger gasMaxUsed = fastGasPrice.multiply(gasLimit);

            // 获得eth余额
            JxResponse ethBalanceRes = getEthBalance(fromAddress);
            if (ethBalanceRes.code() != 0) return ethBalanceRes;
            BigInteger ethBalance = (BigInteger) ethBalanceRes.data();
            if (ethBalance.compareTo(gasMaxUsed) < 0) {
                return JxResponse.error(3, "手续费不足，请核实");
            }

            // 获取私钥信息
            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);
            // 构建交易
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, fastGasPrice, gasLimit, contractAddress, BigInteger.ZERO, funcABI);
            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println(ethSendTransaction.getError().getMessage());
                return JxResponse.error(5, ethSendTransaction.getError().getMessage());
            } else {
                return JxResponse.success(ethSendTransaction);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, "交易异常，请刷新重试");
        }
    }

    // 转账代币
    public static JxResponse transferTokenByNonce(String fromAddress, String toAddress,
                                                  String privateKey, String contractAddress, BigDecimal amount,
                                                  BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice) {
        try {
            // 构建方法调用信息
            String method = "transfer";
            // amount 转换为整数
            JxResponse decimalsRes = getTokenDecimals(contractAddress);
            if (decimalsRes.code() != 0) return decimalsRes;
            int decimals = (int) decimalsRes.data();
            BigInteger bigAmount = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger();
            JxResponse tokenBalanceRes = getTokenBalance(fromAddress, contractAddress);
            if (tokenBalanceRes.code() != 0) return tokenBalanceRes;
            BigInteger tokenBalance = (BigInteger) tokenBalanceRes.data();
            System.out.println("tokenBalance = " + tokenBalance + " bigAmount = " + bigAmount);
            if (tokenBalance.add(bigAmount).compareTo(bigAmount) < 0) {
                return JxResponse.error(2, "代币余额不足，请核实");
            }

            // 构建输入参数
            List<Type> inputArgs = new ArrayList<>();
            inputArgs.add(new Address(toAddress));
            inputArgs.add(new Uint256(bigAmount));
            // 合约返回值容器
            List<TypeReference<?>> outputArgs = new ArrayList<>();
            String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
            BigInteger gasMaxUsed = gasPrice.multiply(gasLimit);

            // 获得eth余额
            JxResponse ethBalanceRes = getEthBalance(fromAddress);
            if (ethBalanceRes.code() != 0) return ethBalanceRes;
            BigInteger ethBalance = (BigInteger) ethBalanceRes.data();
            if (ethBalance.compareTo(gasMaxUsed) < 0) {
                return JxResponse.error(3, "手续费不足，请核实");
            }

            // 获取私钥信息
            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);
            // 构建交易
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, contractAddress, BigInteger.ZERO, funcABI);
            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                System.out.println(ethSendTransaction.getError().getMessage());
                return JxResponse.error(5, ethSendTransaction.getError().getMessage());
            } else {
                return JxResponse.success(ethSendTransaction);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, "交易异常，请刷新重试");
        }
    }

    // 查询代币余额
    public static JxResponse getTokenBalance(String fromAddress, String contractAddress) {
        String methodName = "balanceOf";
        // 输入参数
        List<Type> inputParameters = new ArrayList<>();
        // 输出参数
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        // 地址
        Address address = new Address(fromAddress);
        inputParameters.add(address);

        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        // 编码
        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(fromAddress, contractAddress, data);

        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(results.get(0).getValue());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 将BigInteger通过decimals(usdt 6位精度)转化为BigDecimal
    public static BigDecimal getDecimalBalance(BigInteger balance, int decimals) {
        BigDecimal decimalBalance = new BigDecimal(balance);
        decimalBalance = decimalBalance.divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.HALF_UP);
        return decimalBalance;
    }

    // 查询代币的名称
    public static JxResponse getTokenName(String contractAddress) {
        String methodName = "name";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        TypeReference<Utf8String> typeReference = new TypeReference<Utf8String>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);

        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(emptyAddress, contractAddress, data);
        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).sendAsync().get();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(results.get(0).getValue());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 查询代币符号
    public static JxResponse getTokenSymbol(String contractAddress) {
        String methodName = "symbol";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        TypeReference<Utf8String> typeReference = new TypeReference<Utf8String>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);

        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(emptyAddress, contractAddress, data);

        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).sendAsync().get();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(results.get(0).getValue());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 查询代币精度
    public static JxResponse getTokenDecimals(String contractAddress) {
        String methodName = "decimals";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        TypeReference<Uint8> typeReference = new TypeReference<Uint8>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(emptyAddress, contractAddress, data);

        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).sendAsync().get();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(((BigInteger) results.get(0).getValue()).intValue());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 查询代币发行总量
    public static JxResponse getTokenTotalSupply(String contractAddress) {
        String methodName = "totalSupply";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);

        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(emptyAddress, contractAddress, data);

        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).sendAsync().get();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(results.get(0).getValue());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    // 获取配额
    public static JxResponse getAllowanceBalance(String fromAddress, String toAddress, String contractAddress) {
        String methodName = "allowance";
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(fromAddress));
        inputParameters.add(new Address(toAddress));

        List<TypeReference<?>> outputs = new ArrayList<>();
        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputs.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputs);
        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);
        try {
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).send();
            List<Type> result = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            return JxResponse.success(result.get(0).getValue());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return JxResponse.error(10, e.getMessage());
        }
    }

    public static String getCalldataNoOutput(String methodName, Type... inputParameters) {
        // 输入参数
        List<Type> inputParameterList = Arrays.asList(inputParameters);
        // 返回值的容器
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Function function = new Function(methodName, inputParameterList, outputParameters);
        // 编码
        String data = FunctionEncoder.encode(function);
        System.out.println(data);
        return data;
    }

}
