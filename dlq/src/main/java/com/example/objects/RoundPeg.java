/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.example.objects;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class RoundPeg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 3220886135122734581L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"RoundPeg\",\"namespace\":\"com.example.objects\",\"fields\":[{\"name\":\"diameter\",\"type\":\"int\",\"doc\":\"Diameter of the round peg face\"}],\"version\":\"1\"}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<RoundPeg> ENCODER =
      new BinaryMessageEncoder<RoundPeg>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<RoundPeg> DECODER =
      new BinaryMessageDecoder<RoundPeg>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<RoundPeg> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<RoundPeg> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<RoundPeg> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<RoundPeg>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this RoundPeg to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a RoundPeg from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a RoundPeg instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static RoundPeg fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** Diameter of the round peg face */
  private int diameter;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public RoundPeg() {}

  /**
   * All-args constructor.
   * @param diameter Diameter of the round peg face
   */
  public RoundPeg(java.lang.Integer diameter) {
    this.diameter = diameter;
  }

  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return diameter;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: diameter = (java.lang.Integer)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'diameter' field.
   * @return Diameter of the round peg face
   */
  public int getDiameter() {
    return diameter;
  }


  /**
   * Sets the value of the 'diameter' field.
   * Diameter of the round peg face
   * @param value the value to set.
   */
  public void setDiameter(int value) {
    this.diameter = value;
  }

  /**
   * Creates a new RoundPeg RecordBuilder.
   * @return A new RoundPeg RecordBuilder
   */
  public static com.example.objects.RoundPeg.Builder newBuilder() {
    return new com.example.objects.RoundPeg.Builder();
  }

  /**
   * Creates a new RoundPeg RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new RoundPeg RecordBuilder
   */
  public static com.example.objects.RoundPeg.Builder newBuilder(com.example.objects.RoundPeg.Builder other) {
    if (other == null) {
      return new com.example.objects.RoundPeg.Builder();
    } else {
      return new com.example.objects.RoundPeg.Builder(other);
    }
  }

  /**
   * Creates a new RoundPeg RecordBuilder by copying an existing RoundPeg instance.
   * @param other The existing instance to copy.
   * @return A new RoundPeg RecordBuilder
   */
  public static com.example.objects.RoundPeg.Builder newBuilder(com.example.objects.RoundPeg other) {
    if (other == null) {
      return new com.example.objects.RoundPeg.Builder();
    } else {
      return new com.example.objects.RoundPeg.Builder(other);
    }
  }

  /**
   * RecordBuilder for RoundPeg instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<RoundPeg>
    implements org.apache.avro.data.RecordBuilder<RoundPeg> {

    /** Diameter of the round peg face */
    private int diameter;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.example.objects.RoundPeg.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.diameter)) {
        this.diameter = data().deepCopy(fields()[0].schema(), other.diameter);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
    }

    /**
     * Creates a Builder by copying an existing RoundPeg instance
     * @param other The existing instance to copy.
     */
    private Builder(com.example.objects.RoundPeg other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.diameter)) {
        this.diameter = data().deepCopy(fields()[0].schema(), other.diameter);
        fieldSetFlags()[0] = true;
      }
    }

    /**
      * Gets the value of the 'diameter' field.
      * Diameter of the round peg face
      * @return The value.
      */
    public int getDiameter() {
      return diameter;
    }


    /**
      * Sets the value of the 'diameter' field.
      * Diameter of the round peg face
      * @param value The value of 'diameter'.
      * @return This builder.
      */
    public com.example.objects.RoundPeg.Builder setDiameter(int value) {
      validate(fields()[0], value);
      this.diameter = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'diameter' field has been set.
      * Diameter of the round peg face
      * @return True if the 'diameter' field has been set, false otherwise.
      */
    public boolean hasDiameter() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'diameter' field.
      * Diameter of the round peg face
      * @return This builder.
      */
    public com.example.objects.RoundPeg.Builder clearDiameter() {
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RoundPeg build() {
      try {
        RoundPeg record = new RoundPeg();
        record.diameter = fieldSetFlags()[0] ? this.diameter : (java.lang.Integer) defaultValue(fields()[0]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<RoundPeg>
    WRITER$ = (org.apache.avro.io.DatumWriter<RoundPeg>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<RoundPeg>
    READER$ = (org.apache.avro.io.DatumReader<RoundPeg>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeInt(this.diameter);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.diameter = in.readInt();

    } else {
      for (int i = 0; i < 1; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.diameter = in.readInt();
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










