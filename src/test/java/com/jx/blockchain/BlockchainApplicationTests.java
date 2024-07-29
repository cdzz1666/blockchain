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
import org.bitcoinj.script.ScriptOpCodes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        JxResponse transfer = btcService.estimateTxAmount(
                "tb1qc0vnw6lygkkrxc9h60fhsewzmlpu39njt6dl2w",
                "tb1qfzlvcjw2jmvgwyr8refymgtx4fzasjwzeq8spw",
                BigDecimal.valueOf(0.00002).multiply(BigDecimal.TEN.pow(8)), "");
        System.out.println(transfer.data());
    }

    @Test
    void btcTransfer() throws Exception {
        // tb1qc0vnw6lygkkrxc9h60fhsewzmlpu39njt6dl2w tb1qfzlvcjw2jmvgwyr8refymgtx4fzasjwzeq8spw  P2WPKH
        // mmFfrrhzzycDcVsv84gktJjygsJVEbRANu ms1wFwLt7QaBCH4wDg4f68kbCPFJvqMsRu  P2PKH
        JxResponse txFee = btcService.estimateTxAmount(
                "mmFfrrhzzycDcVsv84gktJjygsJVEbRANu",
                "ms1wFwLt7QaBCH4wDg4f68kbCPFJvqMsRu",
                BigDecimal.valueOf(0.00002).multiply(BigDecimal.TEN.pow(8)), "cN3TVLUWf5nqLPUT1JL3ME48an2mfMFJG9JzLN6U6sR7hjEgoVgg");

        JxResponse transfer = btcService.transfer(
                "mmFfrrhzzycDcVsv84gktJjygsJVEbRANu",
                "ms1wFwLt7QaBCH4wDg4f68kbCPFJvqMsRu",
                BigDecimal.valueOf(0.00002).multiply(BigDecimal.TEN.pow(8)), new BigDecimal(txFee.data().toString()), "cN3TVLUWf5nqLPUT1JL3ME48an2mfMFJG9JzLN6U6sR7hjEgoVgg");
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
    void getUtxo(){
        JxResponse jxResponse = btcService.postGetUtxo("2N8bqgSygfC3s7Vc2t3GhyWruqj1SQKviNh");
    }
    @Test
    void getBalance(){
        JxResponse jxResponse = btcService.getBalance("15NeLd3Fsr8yKwSTbxUqVmz2wGUK48SsX2");
        System.out.println(jxResponse.data());
    }

    @Test
    void getTronTransaction(){
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
}
