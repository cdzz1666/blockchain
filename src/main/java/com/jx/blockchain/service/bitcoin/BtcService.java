package com.jx.blockchain.service.bitcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.jx.blockchain.service.bitcoin.vo.*;
import com.jx.blockchain.utils.ListUtils;
import com.jx.blockchain.vo.*;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.wallet.DeterministicSeed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Service
public class BtcService {
    // 主网
//    private final static String rpcUrl = "";
//    private final static NetworkParameters netParams = MainNetParams.get();
//    private final static int coinType = 0;
    // 测试网
    private final static String rpcUrl = "https://compatible-practical-brook.btc-testnet.quiknode.pro/380ff48fb938e2e6d82571ddecdfdfa14887f8e0/";
    private final static NetworkParameters netParams = TestNet3Params.get();
    private final static int coinType = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @NotNull ImmutableList<ChildNumber> getHdPath(String addressType) {
        int purpose = 44;
        if ("segwit_nested".equals(addressType)) {
            purpose = 49;
        } else if ("segwit_native".equals(addressType)) {
            purpose = 84;
        } else if ("taproot".equals(addressType)) {
            purpose = 86;
        }
        return ImmutableList.of(new ChildNumber(purpose, true),
                new ChildNumber(coinType, true),
                ChildNumber.ZERO_HARDENED, ChildNumber.ZERO);
    }

    /**
     * 新建一个BTC地址
     *
     * @param addressType // Legacy 44" | "segwit_nested 84" | "segwit_native 49"
     *                    "segwit_native 49" | taproot 86"
     * @return JxResponse
     */
    public String getNewAddress(String addressType) {
        try {
            ImmutableList<ChildNumber> hdPath = getHdPath(addressType);
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
                    .deriveChild(hdPath, false, true, new ChildNumber(0));
            byte[] bytes = deterministicKey.getPrivKeyBytes();
            ECKey ecKey = ECKey.fromPrivate(bytes);
            System.out.println(String.join(" ", mnemonicList));
            return getBtcAddress(addressType, ecKey);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    public String getAddressByMnemonic(String mnemonic, String addressType) {
        try {
            List<String> mnemonicList = List.of(mnemonic.split(" "));
            //使用助记词生成钱包种子
            byte[] seed = MnemonicCode.toSeed(mnemonicList, "");
            DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
            DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
            DeterministicKey deterministicKey = deterministicHierarchy
                    .deriveChild(getHdPath(addressType), false, true, new ChildNumber(0));
            byte[] bytes = deterministicKey.getPrivKeyBytes();
            ECKey ecKey = ECKey.fromPrivate(bytes);
            System.out.println(ecKey.getPrivateKeyAsWiF(netParams));
            return getBtcAddress(addressType, ecKey);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    public String getAddressByPrivateKey(String privateKey, String addressType) {
        try {
            // 使用私钥字符串创建ECKey对象
            ECKey ecKey = getEcKeyByPrivateKey(privateKey);
            System.out.println(ecKey.getPublicKeyAsHex());
            return getBtcAddress(addressType, ecKey);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    private ECKey getEcKeyByPrivateKey(String privateKey) {
        ECKey ecKey;
        if (privateKey.length() == 51 || privateKey.length() == 52) {
            DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(netParams, privateKey);
            ecKey = dumpedPrivateKey.getKey();
        } else {
            BigInteger privKey = Base58.decodeToBigInteger(privateKey);
            ecKey = ECKey.fromPrivate(privKey);
        }
        return ecKey;
    }

    private String getBtcAddress(String addressType, ECKey ecKey) {
        if ("Legacy".equals(addressType)) {
            LegacyAddress legacyAddress = LegacyAddress.fromKey(netParams, ecKey);
            System.out.println(legacyAddress.getVersion());
            return legacyAddress.toBase58();
        } else if ("segwit_nested".equals(addressType)) {
            LegacyAddress legacyAddress = LegacyAddress.fromScriptHash(netParams,
                    Utils.sha256hash160(ScriptBuilder
                            .createP2WPKHOutputScript(ecKey.getPubKeyHash())
                            .getProgram()));
            System.out.println(legacyAddress.getVersion());
            return legacyAddress.toBase58();
        } else if ("segwit_native".equals(addressType)) {
            SegwitAddress segwitAddress = SegwitAddress.fromKey(netParams, ecKey);
            System.out.println(segwitAddress.getWitnessVersion());
            return segwitAddress.toBech32();
        } else if ("taproot".equals(addressType)) {
            return "";
        }
        return "";
    }

    /**
     * 查询交易详情
     *
     * @param txid 交易ID
     * @return JxResponse
     */
    public JxResponse getRawTransaction(String txid) {
        try {
            Map<String, Object> paramMap = new LinkedHashMap<>();
            paramMap.put("jsonrpc", "1.0");
            paramMap.put("id", "6");
            paramMap.put("method", "getrawtransaction");
            paramMap.put("params", ListUtils.of(txid, true));
            String requestParameters = JSON.toJSONString(paramMap).trim();
            String res = post(rpcUrl, requestParameters);
            RpcResponse response = JSON.parseObject(res, RpcResponse.class);
            if (response.getError() != null) {
                return JxResponse.error(1, response.getError());
            } else {
                return JxResponse.success(JSON.parseObject(response.getResult()));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }


    public JxResponse getMemoryPoolInfo() {
        try {
            Map<String, Object> paramMap = new LinkedHashMap<>();
            paramMap.put("jsonrpc", "1.0");
            paramMap.put("id", "6");
            paramMap.put("method", "getmempoolinfo");
            String requestParameters = JSON.toJSONString(paramMap).trim();
            String res = post(rpcUrl, requestParameters);
            RpcResponse response = JSON.parseObject(res, RpcResponse.class);
            if (response.getError() != null) {
                return JxResponse.error(1, response.getError());
            } else {
                return JxResponse.success(response.getResult());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    public BigDecimal getFeeRate() {
        JxResponse memoryInfo = getMemoryPoolInfo();
        System.out.println(memoryInfo.data());
        JSONObject jsonObject = JSONObject.parseObject(memoryInfo.data().toString());
        BigDecimal feeRate = (jsonObject.getBigDecimal("minrelaytxfee").add(jsonObject.getBigDecimal("mempoolminfee")))
                .multiply(BigDecimal.TEN.pow(8)).divide(BigDecimal.valueOf(1024), 0, RoundingMode.CEILING);
        if (feeRate.compareTo(BigDecimal.valueOf(5)) < 0) {
            return feeRate.multiply(BigDecimal.valueOf(2));
        }
        return feeRate.add(BigDecimal.ONE);
    }

    /**
     * 获取地址对应余额
     *
     * @param address 地址
     * @return JxResponse data balance
     */
    public JxResponse getBalance(String address) {
        try {
            String api = "https://mempool.space/testnet/api/address/" + address;
            if (netParams instanceof MainNetParams) {
                api = "https://mempool.space/testnet/api/address/" + address;
            }
            Map<String, Object> paramsMap = new HashMap<>();
            Map<String, String> headerMap = new HashMap<>();
            String res = get(api, headerMap, paramsMap);
            JSONObject resObj = JSON.parseObject(res);
            JSONObject chainStats = resObj.getJSONObject("chain_stats");
            String fundedTxoSum = chainStats.getString("funded_txo_sum");
            String spentTxoSum = chainStats.getString("spent_txo_sum");
            return JxResponse.success(new BigDecimal(fundedTxoSum).subtract(new BigDecimal(spentTxoSum)));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return JxResponse.error(500, "网络异常");
        }
    }

    public JxResponse estimateTxAmount(String fromAddress, String toAddress, BigDecimal transferAmount, String privateKey) {
        try {
            BigDecimal txAmount = BigDecimal.ZERO;
            BigDecimal reduceAmount = transferAmount.add(txAmount);
            ECKey ecKey = getEcKeyByPrivateKey(privateKey);
            Address from = Address.fromString(netParams, fromAddress);
            System.out.println(from.getOutputScriptType());
            // 1. 查询源地址下面UTXO
            JxResponse utxoRes = postGetUtxo(fromAddress);
            if (utxoRes.code() != 0) {
                return JxResponse.error(3, utxoRes.data().toString());
            }
            List<Utxo> transferList = JSONArray.parseArray(utxoRes.data().toString(), Utxo.class);

            // 3. createRawTransaction -> signRawTransaction -> sendRawTransaction
            // 3.1 createRawTransaction
            Transaction tx = new Transaction(netParams);

            BigDecimal utxoToalAmount = BigDecimal.ZERO;
            // 创建交易输入
            for (int i = 0; i < transferList.size(); i++) {
                Utxo utxo = transferList.get(i);
                if (utxoToalAmount.compareTo(reduceAmount) > 0) break;
                utxoToalAmount = utxoToalAmount.add(new BigDecimal(utxo.getCoinAmount()));
                TransactionOutPoint outPoint = new TransactionOutPoint(netParams, utxo.getVout(), Sha256Hash.wrap(utxo.getTxid()));
                TransactionInput input = new TransactionInput(netParams, null, new byte[]{}, outPoint, Coin.valueOf(new BigDecimal(utxo.getCoinAmount()).longValue()));
                tx.addInput(input);
                System.out.println(input.getValue());
            }
            BigDecimal remainAmount = utxoToalAmount.subtract(reduceAmount);

            // 创建交易输出
            Address to = Address.fromString(netParams, toAddress);
            tx.addOutput(Coin.valueOf(transferAmount.longValue()), to);

            if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 找零
                Address changeAddress = Address.fromKey(netParams, ecKey, Script.ScriptType.P2WPKH);
                Coin change = Coin.valueOf(remainAmount.longValue());
                tx.addOutput(change, changeAddress);
            }

            // 3.2 signRawTransaction
            List<TransactionInput> transactionInputs = tx.getInputs();
            for (int i = 0; i < transactionInputs.size(); i++) {
                TransactionInput txInput = tx.getInput(i);
                if (from.getOutputScriptType() == Script.ScriptType.P2WPKH) {
                    Script witnessScript = ScriptBuilder.createP2PKHOutputScript(ecKey);
                    Sha256Hash hash = tx.hashForWitnessSignature(i, witnessScript, txInput.getValue(), Transaction.SigHash.ALL, false);
                    ECKey.ECDSASignature ecSig = ecKey.sign(hash);
                    TransactionSignature txSig = new TransactionSignature(ecSig, Transaction.SigHash.ALL, false);
                    txInput.setWitness(TransactionWitness.redeemP2WPKH(txSig, ecKey));
                }
                if (from.getOutputScriptType() == Script.ScriptType.P2PKH) {
                    Sha256Hash hash = tx.hashForSignature(i, ScriptBuilder.createP2PKHOutputScript(ecKey), Transaction.SigHash.ALL, false);
                    ECKey.ECDSASignature ecSig = ecKey.sign(hash);
                    TransactionSignature txSig = new TransactionSignature(ecSig, Transaction.SigHash.ALL, false);
                    txInput.setScriptSig(ScriptBuilder.createInputScript(txSig, ecKey));
                }
            }
            int vSize = tx.getVsize();
            return JxResponse.success(getFeeRate().multiply(BigDecimal.valueOf(vSize)));
        } catch (Exception e) {
            logger.error("exception message", e);
            return JxResponse.error(500, "网络异常");
        }
    }

    public JxResponse transfer(String fromAddress, String toAddress, BigDecimal transferAmount, BigDecimal txAmount, String privateKey) {
        try {
            BigDecimal reduceAmount = transferAmount.add(txAmount);
            ECKey ecKey = getEcKeyByPrivateKey(privateKey);
            Address from = Address.fromString(netParams, fromAddress);
            System.out.println(from.getOutputScriptType());
            // 1. 查询源地址下面UTXO
            JxResponse utxoRes = postGetUtxo(fromAddress);
            if (utxoRes.code() != 0) {
                return JxResponse.error(3, utxoRes.data().toString());
            }
            List<Utxo> transferList = JSONArray.parseArray(utxoRes.data().toString(), Utxo.class);
            if (transferList.isEmpty()) {
                return JxResponse.error(1, "余额不足");
            }
            // 3. createRawTransaction -> signRawTransaction -> sendRawTransaction
            // 3.1 createRawTransaction
            Transaction tx = new Transaction(netParams);

            BigDecimal utxoToalAmount = BigDecimal.ZERO;
            // 创建交易输入
            for (int i = 0; i < transferList.size(); i++) {
                Utxo utxo = transferList.get(i);
                if (utxoToalAmount.compareTo(reduceAmount) > 0) break;
                utxoToalAmount = utxoToalAmount.add(new BigDecimal(utxo.getCoinAmount()));
                TransactionOutPoint outPoint = new TransactionOutPoint(netParams, utxo.getVout(), Sha256Hash.wrap(utxo.getTxid()));
                TransactionInput input = new TransactionInput(netParams, null, new byte[]{}, outPoint, Coin.valueOf(new BigDecimal(utxo.getCoinAmount()).longValue()));
                tx.addInput(input);
                System.out.println(input.getValue());
            }
            BigDecimal remainAmount = utxoToalAmount.subtract(reduceAmount);
            if (remainAmount.compareTo(BigDecimal.ZERO) < 0) {
                return JxResponse.error(1, "余额不足");
            }
            // 创建交易输出
            Address to = Address.fromString(netParams, toAddress);
            tx.addOutput(Coin.valueOf(transferAmount.longValue()), to);

            if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 找零
                Address changeAddress = Address.fromKey(netParams, ecKey, Script.ScriptType.P2WPKH);
                Coin change = Coin.valueOf(remainAmount.longValue());
                tx.addOutput(change, changeAddress);
            }

            // 3.2 signRawTransaction
            List<TransactionInput> transactionInputs = tx.getInputs();
            for (int i = 0; i < transactionInputs.size(); i++) {
                TransactionInput txInput = tx.getInput(i);
                if (from.getOutputScriptType() == Script.ScriptType.P2WPKH) {
                    Script witnessScript = ScriptBuilder.createP2PKHOutputScript(ecKey);
                    Sha256Hash hash = tx.hashForWitnessSignature(i, witnessScript, txInput.getValue(), Transaction.SigHash.ALL, false);
                    ECKey.ECDSASignature ecSig = ecKey.sign(hash);
                    TransactionSignature txSig = new TransactionSignature(ecSig, Transaction.SigHash.ALL, false);
                    txInput.setWitness(TransactionWitness.redeemP2WPKH(txSig, ecKey));
                }
                if (from.getOutputScriptType() == Script.ScriptType.P2PKH) {
                    Sha256Hash hash = tx.hashForSignature(i, ScriptBuilder.createP2PKHOutputScript(ecKey), Transaction.SigHash.ALL, false);
                    ECKey.ECDSASignature ecSig = ecKey.sign(hash);
                    TransactionSignature txSig = new TransactionSignature(ecSig, Transaction.SigHash.ALL, false);
                    txInput.setScriptSig(ScriptBuilder.createInputScript(txSig, ecKey));
                }
            }

            String signHexTransaction = Utils.HEX.encode(tx.bitcoinSerialize());
            System.out.println(signHexTransaction);
            // 3.3 sendRawTransaction
            Map<String, Object> paramMap = new LinkedHashMap<>();
            paramMap.put("jsonrpc", "1.0");
            paramMap.put("id", "6");
            paramMap.put("method", "sendrawtransaction");
            paramMap.put("params", List.of(signHexTransaction));
            String requestParameters = JSON.toJSONString(paramMap).trim();
            String res = post(rpcUrl, requestParameters);
            RpcResponse response = JSON.parseObject(res, RpcResponse.class);
            if (response.getError() != null) {
                return JxResponse.error(5, response.getError());
            }
            String txid = response.getResult();
            System.out.println("txid = " + txid);
            return JxResponse.success(txid);
        } catch (Exception e) {
            logger.error("exception message", e);
            return JxResponse.error(500, "网络异常");
        }
    }

    public JxResponse postGetUtxo(String address) {
        try {
            String api = "https://mempool.space/testnet/api/address/" + address + "/txs";
            if (netParams instanceof MainNetParams) {
                api = "https://mempool.space/api/address/" + address + "/txs";
            }
            Map<String, Object> paramMap = new HashMap<>();
            Map<String, String> headerMap = new HashMap<>();
            String res = get(api, headerMap, paramMap);
            JSONArray memTransactions = JSONArray.parseArray(res);

            // 步骤 1：收集所有 vin 中的 txid
            Set<String> spentTxIds = new HashSet<>();
            for (Object txObj : memTransactions) {
                JSONArray vinList = JSONArray.parseArray(JSONObject.parseObject(txObj.toString()).getString("vin"));
                for (Object vinObj : vinList) {
                    JSONObject vin = JSONObject.parseObject(vinObj.toString());
                    spentTxIds.add(vin.getString("txid"));
                }
            }

            // 步骤 2：筛选出符合条件的 vout
            List<Utxo> result = new ArrayList<>();
            for (Object txObj : memTransactions) {
                JSONObject tx = JSONObject.parseObject(txObj.toString());
                JSONArray voutList = JSONArray.parseArray(tx.getString("vout"));
                for (int i = 0; i < voutList.size(); i++) {
                    Object voutObj = voutList.get(i);
                    JSONObject vout = JSONObject.parseObject(voutObj.toString());
                    if (vout.getString("scriptpubkey_address").equals(address) && !spentTxIds.contains(tx.getString("txid"))) {
                        Utxo unspent = new Utxo();
                        unspent.setVout(i);
                        unspent.setTxid(tx.getString("txid"));
                        unspent.setCoinAmount(vout.getString("value"));
                        result.add(unspent);
                    }
                }
            }
            System.out.println(result);
            return JxResponse.success(result);
        } catch (Exception e) {
            logger.error("exception message", e);
            return JxResponse.error(500, "网络异常");
        }
    }

    public static String post(String url,
                              String payload) throws IOException {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        int timeout = 30000;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");// 提交模式
            conn.setConnectTimeout(timeout);//连接超时 单位毫秒
            conn.setReadTimeout(timeout);//读取超时 单位毫秒
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            ////连接,也可以不用明文connect，使用下面的httpConn.getOutputStream()会自动connect
            if (headerMap != null) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.connect();
            // 获取URLConnection对象对应的输出流
            if (payload != null && !payload.isEmpty()) {
//                System.out.println("payload = " + payload);
                PrintWriter printWriter = new PrintWriter(conn.getOutputStream());
                // 发送请求参数
                printWriter.write(payload);
                // flush输出流的缓冲
                printWriter.flush();
                printWriter.close();
            }
            int responseCode = conn.getResponseCode();
            if (200 == responseCode) {
                //开始获取数据
                return read(conn.getInputStream());
            } else {
                String error = read(conn.getErrorStream());
                System.out.println("code = " + responseCode + ", error = " + error);
                return error;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String read(InputStream is) throws IOException {
        return new String(readBytes(is), StandardCharsets.UTF_8);
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        return readBytes(is, 8192);
    }

    public static String read(InputStream is, Charset charset) throws IOException {
        return new String(readBytes(is), charset);
    }

    public static byte[] readBytes(InputStream is, int bufferSize) throws IOException {
        //开始获取数据
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] arr = new byte[bufferSize];
        while ((len = bis.read(arr)) != -1) {
            bos.write(arr, 0, len);
            bos.flush();
        }
        bos.close();
        return bos.toByteArray();
    }

    public static String get(
            String url,
            Map<String, String> headerMap,
            Map<String, ?> queryMap) throws IOException {
        HttpURLConnection conn = null;
        try {
            if (queryMap != null) {
                StringBuilder querystring = new StringBuilder();
                queryMap.forEach((key, value) -> {
                    querystring.append(key);
                    querystring.append("=");
                    querystring.append(value);
                });
                url = url + "?" + querystring;
            }
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");// 提交模式
            conn.setConnectTimeout(20000);//连接超时 单位毫秒
            conn.setReadTimeout(20000);//读取超时 单位毫秒
            conn.setDoOutput(true);
            conn.setDoInput(true);
            ////连接,也可以不用明文connect，使用下面的httpConn.getOutputStream()会自动connect
            if (headerMap != null) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (200 == responseCode) {
                //开始获取数据
                return read(conn.getInputStream());
            } else {
                String error = read(conn.getErrorStream());
                System.out.println("code = " + responseCode + ", error = " + error);
                return error;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}

class Utxo {
    private String txid;
    private int vout;
    private String coinAmount;

    public Utxo() {
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public int getVout() {
        return vout;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    public String getCoinAmount() {
        return coinAmount;
    }

    public void setCoinAmount(String coinAmount) {
        this.coinAmount = coinAmount;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
