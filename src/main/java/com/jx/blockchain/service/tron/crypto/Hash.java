/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jx.blockchain.service.tron.crypto;
import com.jx.blockchain.service.tron.crypto.jce.TronCastleProvider;
import com.jx.blockchain.service.tron.utils.ByteArray;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

public class Hash {

  private static final Provider CRYPTO_PROVIDER;

  private static final String HASH_256_ALGORITHM_NAME;
  private static final String HASH_512_ALGORITHM_NAME;

  static {
    Security.addProvider(TronCastleProvider.getInstance());
    CRYPTO_PROVIDER = Security.getProvider("BC");
    HASH_256_ALGORITHM_NAME = "TRON-KECCAK-256";
    HASH_512_ALGORITHM_NAME = "TRON-KECCAK-512";
  }

  public static byte[] sha3(byte[] input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
              CRYPTO_PROVIDER);
      digest.update(input);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Can't find such algorithm" + e);
      throw new RuntimeException(e);
    }

  }

  /**
   * Keccak-256 hash function.
   *
   * @param hexInput hex encoded input data with optional 0x prefix
   * @return hash value as hex encoded string
   */
  public static String sha3(String hexInput) {
    byte[] bytes = ByteArray.fromHexString(hexInput);
    byte[] result = sha3(bytes);
    return ByteArray.toHexString(result);
  }


  public static byte[] sha3(byte[] input1, byte[] input2) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
              CRYPTO_PROVIDER);
      digest.update(input1, 0, input1.length);
      digest.update(input2, 0, input2.length);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Can't find such algorithm" + e);
      throw new RuntimeException(e);
    }
  }

  /**
   * hashing chunk of the data
   *
   * @param input - data for hash
   * @param start - start of hashing chunk
   * @param length - length of hashing chunk
   * @return - keccak hash of the chunk
   */
  public static byte[] sha3(byte[] input, int start, int length) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
              CRYPTO_PROVIDER);
      digest.update(input, start, length);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Can't find such algorithm" + e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] sha512(byte[] input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_512_ALGORITHM_NAME,
              CRYPTO_PROVIDER);
      digest.update(input);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Can't find such algorithm" + e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates RIGTMOST160(SHA3(input)). This is used in address calculations. *
   *
   * @param input - data
   * @return - add_pre_fix + 20 right bytes of the hash keccak of the data
   */
//  public static byte[] sha3omit12(byte[] input) {
//    byte[] hash = sha3(input);
//    byte[] address = copyOfRange(hash, 11, hash.length);
//    address[0] = WalletApi.getAddressPreFixByte();
//    return address;
//  }
}
