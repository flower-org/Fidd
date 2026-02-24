package com.fidd.core.encryption.aes256;

import org.bouncycastle.crypto.engines.GOST3412_2015Engine;
import org.bouncycastle.crypto.modes.G3413CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

public class KuznechikCbcEncryptionAlgorithm extends KuznechikBase {
  @Override
  PaddedBufferedBlockCipher newCipher() {
    return new PaddedBufferedBlockCipher(new G3413CBCBlockCipher(new GOST3412_2015Engine()));
  }

  @Override
  public String name() {
    return "KUZNECHIK-CBC";
  }
}
