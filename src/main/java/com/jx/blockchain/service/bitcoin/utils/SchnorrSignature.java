package com.jx.blockchain.service.bitcoin.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static com.jx.blockchain.service.bitcoin.BtcService.liftX;

public class SchnorrSignature {

    // 椭圆曲线参数
    private static final BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    private static final BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final Point G = new Point(
            new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16),
            new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
    );

    public static class Point {
        public final BigInteger x;
        public final BigInteger y;

        public Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static byte[] bytesFromInt(BigInteger a) {
        byte[] result = new byte[32];
        byte[] bytes = a.toByteArray();
        if (bytes.length > 32) {
            System.arraycopy(bytes, bytes.length - 32, result, 0, 32);
        } else {
            System.arraycopy(bytes, 0, result, 32 - bytes.length, bytes.length);
        }
        return result;
    }

    public static BigInteger intFromBytes(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] taggedHash(String tag, byte[] msg) {
        byte[] tagHash = sha256(tag.getBytes());
        return sha256(concat(tagHash, tagHash, msg));
    }

    public static Point pointAdd(Point P1, Point P2) {
        if (P1 == null) return P2;
        if (P2 == null) return P1;
        if (P1.x.equals(P2.x) && !P1.y.equals(P2.y)) return null;

        BigInteger lam;
        if (P1.equals(P2)) {
            lam = P1.x.pow(2).multiply(BigInteger.valueOf(3)).multiply(P1.y.multiply(BigInteger.valueOf(2)).modInverse(p)).mod(p);
        } else {
            lam = P2.y.subtract(P1.y).multiply(P2.x.subtract(P1.x).modInverse(p)).mod(p);
        }
        BigInteger x3 = lam.pow(2).subtract(P1.x).subtract(P2.x).mod(p);
        BigInteger y3 = lam.multiply(P1.x.subtract(x3)).subtract(P1.y).mod(p);
        return new Point(x3, y3);
    }

    public static Point pointMul(Point P, BigInteger d) {
        Point R = null;
        for (int i = 0; i < 256; i++) {
            if (d.testBit(i)) {
                R = pointAdd(R, P);
            }
            P = pointAdd(P, P);
        }
        return R;
    }

    public static byte[] schnorrSign(byte[] msg, String privateKeyHex) throws Exception {
        if (msg.length != 32) {
            throw new IllegalArgumentException("The message must be a 32-byte array.");
        }

        BigInteger d0 = new BigInteger(privateKeyHex, 16);
        if (d0.compareTo(BigInteger.ONE) < 0 || d0.compareTo(n.subtract(BigInteger.ONE)) > 0) {
            throw new IllegalArgumentException("The secret key must be an integer in the range 1..n-1.");
        }

        Point P = pointMul(G, d0);
        if (P == null) {
            throw new Exception("Point multiplication failed.");
        }

        BigInteger d = hasEvenY(P) ? d0 : n.subtract(d0);
        byte[] t = xorBytes(bytesFromBigInt(d), taggedHash("BIP0340/aux", getAuxRand()));
        BigInteger k0 = new BigInteger(1, taggedHash("BIP0340/nonce", concat(t, bytesFromPoint(P), msg))).mod(n);
        if (k0.equals(BigInteger.ZERO)) {
            throw new RuntimeException("Failure. This happens only with negligible probability.");
        }

        Point R = pointMul(G, k0);
        if (R == null) {
            throw new Exception("Point multiplication failed.");
        }

        BigInteger k = hasEvenY(R) ? k0 : n.subtract(k0);
        BigInteger e = new BigInteger(1, taggedHash("BIP0340/challenge", concat(bytesFromPoint(R), bytesFromPoint(P), msg))).mod(n);
        byte[] sig = concat(bytesFromPoint(R), bytesFromBigInt(k.add(e.multiply(d)).mod(n)));

        if (!schnorrVerify(msg, bytesFromPoint(P), sig)) {
            throw new RuntimeException("The created signature does not pass verification.");
        }

        return sig;
    }

    private static byte[] bytesFromPoint(Point P) {
        return bytesFromBigInt(P.x);
    }

    static byte[] bytesFromBigInt(BigInteger value) {
        byte[] byteArray = value.toByteArray();
        if (byteArray.length > 32) {
            byte[] trimmedArray = new byte[32];
            System.arraycopy(byteArray, byteArray.length - 32, trimmedArray, 0, 32);
            return trimmedArray;
        }
        return byteArray;
    }

    public static boolean schnorrVerify(byte[] msg, byte[] pubkey, byte[] sig) {
        if (msg.length != 32 || pubkey.length != 32 || sig.length != 64) {
            return false;
        }
        Point P = liftX(pubkey);
        if (P == null) return false;
        BigInteger r = intFromBytes(Arrays.copyOfRange(sig, 0, 32));
        BigInteger s = intFromBytes(Arrays.copyOfRange(sig, 32, 64));
        BigInteger e = intFromBytes(taggedHash("BIP0340/challenge", concat(Arrays.copyOfRange(sig, 0, 32), pubkey, msg))).mod(n);
        Point R = pointAdd(pointMul(G, s), pointMul(P, n.subtract(e)));
        return R != null && hasEvenY(R) && r.equals(R.x);
    }

    private static boolean hasEvenY(Point P) {
        return P.y.mod(BigInteger.TWO).equals(BigInteger.ZERO);
    }

    private static byte[] getAuxRand() {
        byte[] rand = new byte[32];
        new SecureRandom().nextBytes(rand);
        return rand;
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


    public static byte[] xorBytes(byte[] b0, byte[] b1) {
        byte[] result = new byte[b0.length];
        for (int i = 0; i < b0.length; i++) {
            result[i] = (byte) (b0[i] ^ b1[i]);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String privateKey = "d5d3a71a06e45f9c215f764612b209cc41d086651a0aa62f78551408cc64ec2b";
        byte[] msg = sha256("Hello, Schnorr!".getBytes());

        byte[] signature = schnorrSign(msg, privateKey);
        System.out.println("Signature: " + bytesToHex(signature));

        boolean isValid = schnorrVerify(msg, bytesFromInt(pointMul(G, new BigInteger(privateKey, 16)).x), signature);
        System.out.println("Signature valid: " + isValid);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
