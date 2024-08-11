package com.jx.blockchain.service.bitcoin.utils;

import com.jx.blockchain.service.bitcoin.BtcService;
import org.bitcoinj.core.*;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

public class TaprootSigHashBuilder {

    public static String createTaprootMessageHex(NetworkParameters netParams, Transaction tx, Map<String, String> txidVoutScriptPub, int index) throws IOException {
        TransactionInput[] inputs = tx.getInputs().toArray(new TransactionInput[0]);
        TransactionOutput[] outputs = tx.getOutputs().toArray(new TransactionOutput[0]);
        String epoch = "00",
                hash_type = "00",
                version = "01000000",
                lock_time = "00000000",
                sha_prevouts = "",
                sha_amounts = "",
                sha_scriptpubkeys = "",
                sha_sequences = "",
                sha_outputs = "",
                spend_type = "00",
                input_index = "00000000";
        input_index = "0" + index;
        input_index += "0".repeat(8 - input_index.length());
        sha_prevouts = Utils.HEX.encode(computeShaPrevouts(inputs));
        sha_amounts = Utils.HEX.encode(computeShaAmounts(inputs));
        sha_scriptpubkeys = Utils.HEX.encode(computeShaScriptPubKeys(inputs, txidVoutScriptPub));
        sha_sequences = Utils.HEX.encode(computeShaSequences(inputs));
        sha_outputs = Utils.HEX.encode(computeShaOutputs(outputs, netParams));
        String messageHex = epoch + hash_type + version + lock_time + sha_prevouts + sha_amounts + sha_scriptpubkeys + sha_sequences + sha_outputs + spend_type +
                input_index;
        System.out.println("epoch:" + epoch);
        System.out.println("hash_type:" + hash_type);
        System.out.println("version:" + version);
        System.out.println("lock_time:" + lock_time);
        System.out.println("sha_prevouts:" + sha_prevouts);
        System.out.println("sha_amounts:" + sha_amounts);
        System.out.println("sha_scriptpubkeys:" + sha_scriptpubkeys);
        System.out.println("sha_sequences:" + sha_sequences);
        System.out.println("sha_outputs:" + sha_outputs);
        System.out.println("spend_type:" + spend_type);
        System.out.println("input_index:" + input_index);
        System.out.println(messageHex);
        return hashForSign(messageHex);
    }

    public static String hashForSign(String messageHex) {
        byte[] sig_to_hash = Utils.HEX.decode(messageHex);
        byte[] tag_hash = sha256("TapSighash".getBytes());
        byte[] preimage_hash = sha256(BtcService.concatBytes(tag_hash, tag_hash, sig_to_hash));
        return Utils.HEX.encode(preimage_hash);
    }

    public static byte[] computeShaPrevouts(TransactionInput[] inputs) {
        byte[] prevouts = new byte[0];
        for (TransactionInput input : inputs) {
            prevouts = concat(prevouts, reverseByteOrder(input.getOutpoint().getHash().getBytes()));
            prevouts = concat(prevouts, intToLittleEndianBytes((int) input.getOutpoint().getIndex(), 4));
        }
        Sha256Hash.hash(prevouts);
        return sha256(prevouts);
    }

    public static byte[] computeShaAmounts(TransactionInput[] inputs) throws IOException {
        byte[] amounts = new byte[0];
        for (TransactionInput input : inputs) {
            amounts = concat(amounts, longToLittleEndianBytes(input.getValue().value));
        }
        return sha256(amounts);
    }

    public static byte[] computeShaScriptPubKeys(TransactionInput[] inputs, Map<String, String> txidVoutScriptPub) throws IOException {
        byte[] scriptpubkeys = new byte[0];
        for (TransactionInput input : inputs) {
            String scriptPubKey = txidVoutScriptPub.get(input.getOutpoint().getHash() + ":" + input.getOutpoint().getIndex());
            byte[] scriptpubkeyBytes = hexToBytes(scriptPubKey);
            scriptpubkeys = concat(scriptpubkeys, varintLen(scriptpubkeyBytes));
            scriptpubkeys = concat(scriptpubkeys, scriptpubkeyBytes);
        }
        return sha256(scriptpubkeys);
    }

    public static byte[] computeShaSequences(TransactionInput[] inputs) throws IOException {

        // 构建 sha_sequences
        byte[] sequences = new byte[0];
        byte[] bytes = intToLittleEndianBytes(0xffffffff, 4);
        for (TransactionInput input : inputs) {
            input.setSequenceNumber(0xffffffff);
            sequences = concat(sequences, bytes);
        }
        return sha256(sequences);
    }

    public static byte[] computeShaOutputs(TransactionOutput[] outputs, NetworkParameters netParams) throws IOException {
        // 构建 sha_outputs
        byte[] soutputs = new byte[0];
        for (TransactionOutput output : outputs) {
            byte[] scriptPubKey = p2trAddressToScriptPubKey(output.getScriptPubKey().getToAddress(netParams).toString());
            System.out.println("scriptPubKey = " + Utils.HEX.encode(scriptPubKey));
            soutputs = concat(soutputs, longToLittleEndianBytes(output.getValue().value));
            soutputs = concat(soutputs, varintLen(scriptPubKey));
            soutputs = concat(soutputs, scriptPubKey);
            System.out.println("soutputs = " + Utils.HEX.encode(sha256(soutputs)));
        }
        return sha256(soutputs);
    }

    // Convert integer to byte array (little-endian)
    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] doubleSHA256(byte[] input) {
        SHA256Digest digest = new SHA256Digest();
        byte[] firstHash = new byte[digest.getDigestSize()];
        digest.update(input, 0, input.length);
        digest.doFinal(firstHash, 0);

        byte[] secondHash = new byte[digest.getDigestSize()];
        digest.update(firstHash, 0, firstHash.length);
        digest.doFinal(secondHash, 0);

        return secondHash;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] p2trAddressToScriptPubKey(String address) {
        // 检查地址是否为有效的 P2TR 地址
        if (!address.startsWith("tb1p") && !address.startsWith("bc1p") && !address.startsWith("bcrt1p")) {
            throw new IllegalArgumentException("Invalid p2tr address");
        }

        // 解码 Bech32 地址
        Bech32.Bech32Data decoded = Bech32.decode(address);
        if (decoded.data == null) {
            throw new IllegalArgumentException("Bech32 decode failed");
        }

        // 将 5 位比特的编码转换为 8 位比特
        byte[] converted = convertBits(decoded.data, 1, decoded.data.length - 1, 5, 8, false);

        // 返回脚本公钥
        return hexToBytes("5120" + Utils.HEX.encode(converted));
    }

    private static byte[] convertBits(final byte[] in, final int inStart, final int inLen, final int fromBits,
                                      final int toBits, final boolean pad) throws AddressFormatException {
        int acc = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        final int maxv = (1 << toBits) - 1;
        final int max_acc = (1 << (fromBits + toBits - 1)) - 1;
        for (int i = 0; i < inLen; i++) {
            int value = in[i + inStart] & 0xff;
            if ((value >>> fromBits) != 0) {
                throw new AddressFormatException(
                        String.format("Input value '%X' exceeds '%d' bit size", value, fromBits));
            }
            acc = ((acc << fromBits) | value) & max_acc;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out.write((acc >>> bits) & maxv);
            }
        }
        if (pad) {
            if (bits > 0)
                out.write((acc << (toBits - bits)) & maxv);
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new AddressFormatException("Could not convert bits, invalid padding");
        }
        return out.toByteArray();
    }

    public static byte[] varintLen(byte[] data) {
        int length = data.length;
        if (length < 0xfd) {
            return new byte[]{(byte) length};
        } else if (length <= 0xffff) {
            ByteBuffer buffer = ByteBuffer.allocate(3);
            buffer.put((byte) 0xfd);
            buffer.putShort((short) length);
            return buffer.array();
        } else {
            throw new IllegalArgumentException("Data too long, unsupported varint length.");
        }
    }

    public static byte[] longToLittleEndianBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        return buffer.array();
    }

    public static byte[] reverseByteOrder(byte[] input) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[input.length - 1 - i];
        }
        return result;
    }

    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] intToLittleEndianBytes(int value, int byteSize) {
        ByteBuffer buffer = ByteBuffer.allocate(byteSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        return buffer.array();
    }

    public static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}


