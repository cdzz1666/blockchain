package com.jx.blockchain;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.jx.blockchain.service.bitcoin.BtcService;
import com.jx.blockchain.service.ethereum.EthService;
import com.jx.blockchain.service.tron.TronService;
import com.jx.blockchain.service.tron.vo.TronWallet;
import com.jx.blockchain.vo.JxResponse;
import jakarta.annotation.Resource;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@SpringBootTest
class BlockchainApplicationTests {

    @Resource
    TronService tronService;
    @Resource
    BtcService btcService;

    @Test
    void newTronAddress() throws Exception {
        TronWallet wallet = tronService.createWallet();
        System.out.println(wallet);
        // TronWallet{
        // address='TSL8K5YWKFnUBmRf6hBajw95ftyuYLJJxv',
        // privateKey='',
        // mnemonic='surprise upset guitar scale velvet work mouse prefer private midnight cash inside'}
    }

    @Test
    void getTronAddressByPrivateKey() throws Exception {
        String address = tronService.getAddressByPrivateKey("6861663e1be47bbfc08a6ecde483b8cbfba7586850f0f5f555a00408f3f814a6");
        System.out.println(address);
    }

    @Test
    void estimateBtcTxFee() throws Exception {
        // tb1qc0vnw6lygkkrxc9h60fhsewzmlpu39njt6dl2w tb1qfzlvcjw2jmvgwyr8refymgtx4fzasjwzeq8spw  P2WPKH
        // mmFfrrhzzycDcVsv84gktJjygsJVEbRANu ms1wFwLt7QaBCH4wDg4f68kbCPFJvqMsRu  P2PKH
        JxResponse vsizeRes = btcService.calculateVsize(
                "tb1pvwl7ppkkvfe8kc5xt22s5qwlze2su6zxhlzwq8rk8eya3zckhpjslks0hx",
                "tb1pc84zlce2vetgwz4yv5wqkgj4kmngv68m3re8t3tuyz40xmwh7vss6jv86p",
                BigDecimal.valueOf(0.00002).multiply(BigDecimal.TEN.pow(8)),
                "cQXVsVscHk9ox8L8yC6bzXfnhCor9CAWbbGcYU2cwsGr4D7X79pW");

        System.out.println(vsizeRes.data());
        BigDecimal feeRate = btcService.getFeeRate();
        System.out.println(feeRate);
        BigDecimal feeAmount = feeRate.multiply(new BigDecimal(vsizeRes.data().toString()));
        System.out.println(feeAmount);
    }

    @Test
    void btcTransfer() throws Exception {
        String fromAddress = "tb1pc84zlce2vetgwz4yv5wqkgj4kmngv68m3re8t3tuyz40xmwh7vss6jv86p";
        String toAddress = "tb1pvwl7ppkkvfe8kc5xt22s5qwlze2su6zxhlzwq8rk8eya3zckhpjslks0hx";
        String privateKey = "cTnCnSsJKq61G84XGWVnnhg1rX1XxFJbKqK5MQE476u9XEEKZcVT";
        BigDecimal transferAmount = new BigDecimal("0.00002");
        JxResponse vsizeRes = btcService.calculateVsize(
                fromAddress,
                toAddress,
                transferAmount.multiply(BigDecimal.TEN.pow(8)),
                privateKey);

        System.out.println(vsizeRes.data());
        BigDecimal feeRate = btcService.getFeeRate();
        System.out.println(feeRate);
        BigDecimal feeAmount = feeRate.multiply(new BigDecimal(vsizeRes.data().toString()));
        System.out.println(feeAmount);
        JxResponse transfer = btcService.transfer(
                fromAddress,
                toAddress,
                transferAmount.multiply(BigDecimal.TEN.pow(8)),
//                new BigDecimal(txFee.data().toString()),
                feeAmount,
                privateKey);
        System.out.println(transfer.data());
    }

    @Test
    void getNewBtcAddress() {
//        System.out.println(btcService.getNewAddress("segwit_nested"));
        System.out.println(btcService.getAddressByMnemonic("ripple price satoshi soft vault first enjoy elephant merge hair ribbon sweet",
                "Legacy"));
        System.out.println(btcService.getAddressByMnemonic("ripple price satoshi soft vault first enjoy elephant merge hair ribbon sweet",
                "segwit_nested"));
        System.out.println(btcService.getAddressByMnemonic("ripple price satoshi soft vault first enjoy elephant merge hair ribbon sweet",
                "segwit_native"));
        System.out.println(btcService.getAddressByMnemonic("ripple price satoshi soft vault first enjoy elephant merge hair ribbon sweet",
                "taproot"));
    }

    @Test
    void getMemoryInfo() {
        JxResponse memoryInfo = btcService.getMemoryPoolInfo();
        System.out.println(memoryInfo.data());
        JSONObject jsonObject = JSONObject.parseObject(memoryInfo.data().toString());
        BigDecimal minFeeRate =
                (jsonObject.getBigDecimal("minrelaytxfee").add(jsonObject.getBigDecimal("mempoolminfee")))
                        .multiply(BigDecimal.TEN.pow(8)).divide(BigDecimal.valueOf(1024), 0, RoundingMode.CEILING);
        System.out.println(minFeeRate);
    }

    @Test
    void getRawTransaction() {
        JxResponse rawTransaction = btcService.getRawTransaction("c6227a692f94a095a31ff4812501e6591911341b62533150bc9425402fba556f");
        System.out.println(rawTransaction.data());
    }

    @Test
    void outputType() {

        System.out.println(Address.fromString(TestNet3Params.get(), "tb1qc0vnw6lygkkrxc9h60fhsewzmlpu39njt6dl2w").getOutputScriptType());
        System.out.println(Address.fromString(TestNet3Params.get(), "mmFfrrhzzycDcVsv84gktJjygsJVEbRANu").getOutputScriptType());
    }

    @Test
    void getUtxo() {
        JxResponse jxResponse = btcService.postGetUtxo("tb1pc84zlce2vetgwz4yv5wqkgj4kmngv68m3re8t3tuyz40xmwh7vss6jv86p");
        System.out.println(jxResponse.data());
        JxResponse balanceRes = btcService.getBalance("tb1pc84zlce2vetgwz4yv5wqkgj4kmngv68m3re8t3tuyz40xmwh7vss6jv86p");
        System.out.println(balanceRes.data());
    }

    @Test
    void getBalance() {
        JxResponse jxResponse = btcService.getBalance("tb1pc84zlce2vetgwz4yv5wqkgj4kmngv68m3re8t3tuyz40xmwh7vss6jv86p");
        System.out.println(jxResponse.data());
    }

    @Test
    void getTronTransaction() {
        JxResponse transactionInfoById = tronService.getTransactionByIdSolidity("e1d2738d1c0e5278aa0e8628fa96589cb500325351593c73f1b485073d5a6dc9");
        Response.TransactionInfo transactionInfo = (Response.TransactionInfo) transactionInfoById.data();
        System.out.println(Response.TransactionInfo.code.SUCESS.equals(transactionInfo.getResult()));
        System.out.println(Response.TransactionInfo.code.getDescriptor());
        System.out.println(transactionInfo);
        System.out.println(transactionInfo.getReceipt());
        System.out.println(transactionInfo.getResult());
        System.out.println(transactionInfo.getReceipt().getResult());
        boolean equals = Chain.Transaction.Result.contractResult.SUCCESS.equals(transactionInfo.getReceipt().getResult());
        System.out.println(equals);
    }

    @Test
    void btcP2WSH() {
//
//        DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(TestNet3Params.get(), "cQXVsVscHk9ox8L8yC6bzXfnhCor9CAWbbGcYU2cwsGr4D7X79pW");
//        ECKey ecKey = dumpedPrivateKey.getKey();
//
//        Script redeemScript = ScriptBuilder
//                .createP2WPKHOutputScript(ecKey.getPubKeyHash());
//        Script lockingScript = ScriptBuilder.createP2SHOutputScript(redeemScript);
//        System.out.println(redeemScript);
//        System.out.println(lockingScript);
//        System.out.println(Utils.HEX.encode(redeemScript.getProgram()));
//        System.out.println(Utils.HEX.encode(lockingScript.getProgram()));

        Address address = Address.fromString(TestNet3Params.get(), "tb1pvwl7ppkkvfe8kc5xt22s5qwlze2su6zxhlzwq8rk8eya3zckhpjslks0hx");
        System.out.println(address);
        System.out.println(address.getOutputScriptType());
        byte[] sig_to_hash = Utils.HEX.decode("00000100000000000000df4ab7bc35fec96c25cb97506fd7573ebc2954d8ee7fd62a147763b87a15470cc86690d960173932b6a3be70f64b7b7e43b554b555f4cb809faf0e3cb621c98f01331bcaa5e5af1bf32f9f27d5f4197b1fa08e61f6ca5ef2e5c8073fe8b65bdc82d397cbbcff87bc5d0c4c70e424f9b830efbad7bf0be479da5d1d1bafdb97985ad5d604b39841133033667abcdb9628ca335b4b8ff0bde0f920a0edb333c3c50000000000");
        byte[] tag_hash = sha256("TapSighash".getBytes());
        byte[] preimage_hash = sha256(BtcService.concatBytes(tag_hash, tag_hash, sig_to_hash));
        System.out.println(Utils.HEX.encode(preimage_hash));
    }
}
