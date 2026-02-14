package com.fidd.core.encryption.aes256;

public class KuznechikCbcEncryptionAlgorithm extends KuznechikBase {
  public static final String KUZNECHIK_CBC_PKCS_5_PADDING = "GOST3412-2015/CBC/PKCS5Padding";

  @Override
  String transform() {
    return KUZNECHIK_CBC_PKCS_5_PADDING;
  }

  @Override
  public String name() {
    return "KUZNECHIK-CBC";
  }
}
