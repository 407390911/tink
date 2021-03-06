// Copyright 2018 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink;

import com.google.crypto.tink.annotations.Alpha;
import com.google.crypto.tink.proto.KeyData;
import com.google.crypto.tink.proto.KeyData.KeyMaterialType;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import java.security.GeneralSecurityException;

/**
 * A utility class to implement a {@code KeyManager}.
 *
 * <p>Implementing many of the methods in the {@code KeyManager} can be repetitive. This is an
 * internal utility class to implement these methods. In order to instantiate it, one calls
 * the constructor with class objects of the protos used and the type URL:
 *
 * <pre> {@code
 * class MyConcreteKeyManager
 *     extends KeyManagerBase<ConcretePrimitive, ConcreteKeyProto, ConcreteKeyFormatProto> {
 *   public MyConcreteKeyManager() {
 *     super(ConcreteKeyProto.class, ConcreteKeyFormatProto.class, TYPE_URL);
 *   }
 *   [...]
 * }</pre>
 * Furthermore, one implements all the abstract methods in this class.
 *
 * This code is currently Alpha and may change without warning.
 */

@Alpha
public abstract class KeyManagerBase<
        P, KeyProto extends MessageLite, KeyFormatProto extends MessageLite>
    implements KeyManager<P> {
  protected KeyManagerBase(
      final Class<P> primitiveClass,
      final Class<KeyProto> keyProtoClass,
      final Class<KeyFormatProto> keyFormatProtoClass,
      final String typeUrl) {
    this.primitiveClass = primitiveClass;
    this.keyProtoClass = keyProtoClass;
    this.keyFormatProtoClass = keyFormatProtoClass;
    this.typeUrl = typeUrl;
  }

  private final Class<P> primitiveClass;
  private final Class<KeyProto> keyProtoClass;
  private final Class<KeyFormatProto> keyFormatProtoClass;
  private final String typeUrl;

  @SuppressWarnings("unchecked")
  private static <Casted> Casted castOrSecurityException(
      Object objectToCast, String exceptionText, Class<Casted> classObject)
      throws GeneralSecurityException {
    if (!classObject.isInstance(objectToCast)) {
      throw new GeneralSecurityException(exceptionText);
    }
    return (Casted) objectToCast;
  }

  @Override
  public final P getPrimitive(ByteString serializedKey) throws GeneralSecurityException {
    try {
      KeyProto keyProto = parseKeyProto(serializedKey);
      return getPrimitiveFromKey(keyProto);
    } catch (InvalidProtocolBufferException e) {
      throw new GeneralSecurityException(
          "Failures parsing proto of type " + keyProtoClass.getName(), e);
    }
  }

  @Override
  public final P getPrimitive(MessageLite key) throws GeneralSecurityException {
    return getPrimitiveFromKey(
        castOrSecurityException(
            key, "Expected proto of type " + keyProtoClass.getName(), keyProtoClass));
  }

  /**
   * @param serializedKeyFormat serialized {@code AesGcmKeyFormat} proto
   * @return new {@code AesGcmKey} proto
   */
  @Override
  public final MessageLite newKey(ByteString serializedKeyFormat) throws GeneralSecurityException {
    try {
      KeyFormatProto format = parseKeyFormatProto(serializedKeyFormat);
      return newKeyFromFormat(format);
    } catch (InvalidProtocolBufferException e) {
      throw new GeneralSecurityException(
          "Failures parsing proto of type " + keyFormatProtoClass.getName(), e);
    }
  }

  /**
   * @param keyFormat {@code AesGcmKeyFormat} proto
   * @return new {@code AesGcmKey} proto
   */
  @Override
  public final MessageLite newKey(MessageLite keyFormat) throws GeneralSecurityException {
    return newKeyFromFormat(
        castOrSecurityException(
            keyFormat,
            "Expected proto of type " + keyFormatProtoClass.getName(),
            keyFormatProtoClass));
  }

  @Override
  public final boolean doesSupport(String typeUrl) {
    return typeUrl.equals(getKeyType());
  }

  @Override
  public final String getKeyType() {
    return typeUrl;
  }

  /**
   * @param serializedKeyFormat serialized {@code AesGcmKeyFormat} proto
   * @return {@code KeyData} proto with a new {@code AesGcmKey} proto
   */
  @Override
  public final KeyData newKeyData(ByteString serializedKeyFormat) throws GeneralSecurityException {
    KeyFormatProto format;
    try {
      format = parseKeyFormatProto(serializedKeyFormat);
    } catch (InvalidProtocolBufferException e) {
      throw new GeneralSecurityException("Unexpected proto", e);
    }
    KeyProto key = newKeyFromFormat(format);
    return KeyData.newBuilder()
        .setTypeUrl(getKeyType())
        .setValue(key.toByteString())
        .setKeyMaterialType(keyMaterialType())
        .build();
  }

  @Override
  public final Class<P> getPrimitiveClass() {
    return primitiveClass;
  }

  /**
   * Returns the {@code KeyMaterialType} for this proto.
   */
  protected abstract KeyMaterialType keyMaterialType();

  /**
   * Creates a primitive from a given key. The given {@code keyProto} has not been validated.
   */
  protected abstract P getPrimitiveFromKey(KeyProto keyProto) throws GeneralSecurityException;

  /**
   * Creates a new key for a given format. The given {@code keyFormatProto} has not been validated.
   */
  protected abstract KeyProto newKeyFromFormat(KeyFormatProto keyFormatProto)
      throws GeneralSecurityException;

  /**
   * Parses a serialized key proto.
   *
   * Should be implemented as {code return MyKeyProto.parseFrom(byteString);}.
   */
  protected abstract KeyProto parseKeyProto(ByteString byteString)
      throws InvalidProtocolBufferException;

  /**
   * Parses a serialized key format proto.
   *
   * Should be implemented as {code return MyKeyFormatProto.parseFrom(byteString);}.
   */
  protected abstract KeyFormatProto parseKeyFormatProto(ByteString byteString)
      throws InvalidProtocolBufferException;
}
