package com.jx.blockchain.service.tron.crypto;

public interface SignInterface {

  byte[] getPrivateKey();

  byte[] getPubKey();

  byte[] getAddress();

  String signHash(byte[] hash);

  byte[] getNodeId();

  byte[] Base64toBytes(String signature);

  byte[] getPrivKeyBytes();

  SignatureInterface sign(byte[] hash);
}
