// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: NameNodeRpcModel.proto

package com.macfu.dfs.namenode.rpc.model;

/**
 * Protobuf type {@code com.macfu.dfs.namenode.rpc.HeartbeatResponse}
 */
public  final class HeartbeatResponse extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:com.macfu.dfs.namenode.rpc.HeartbeatResponse)
    HeartbeatResponseOrBuilder {
  // Use HeartbeatResponse.newBuilder() to construct.
  private HeartbeatResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private HeartbeatResponse() {
    status_ = 0;
    commands_ = "";
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private HeartbeatResponse(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    int mutable_bitField0_ = 0;
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!input.skipField(tag)) {
              done = true;
            }
            break;
          }
          case 8: {

            status_ = input.readInt32();
            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            commands_ = s;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.internal_static_com_macfu_dfs_namenode_rpc_HeartbeatResponse_descriptor;
  }

  protected FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.internal_static_com_macfu_dfs_namenode_rpc_HeartbeatResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.class, com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.Builder.class);
  }

  public static final int STATUS_FIELD_NUMBER = 1;
  private int status_;
  /**
   * <code>optional int32 status = 1;</code>
   */
  public int getStatus() {
    return status_;
  }

  public static final int COMMANDS_FIELD_NUMBER = 2;
  private volatile Object commands_;
  /**
   * <code>optional string commands = 2;</code>
   */
  public String getCommands() {
    Object ref = commands_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      commands_ = s;
      return s;
    }
  }
  /**
   * <code>optional string commands = 2;</code>
   */
  public com.google.protobuf.ByteString
      getCommandsBytes() {
    Object ref = commands_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (String) ref);
      commands_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (status_ != 0) {
      output.writeInt32(1, status_);
    }
    if (!getCommandsBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, commands_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (status_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, status_);
    }
    if (!getCommandsBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, commands_);
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.macfu.dfs.namenode.rpc.model.HeartbeatResponse)) {
      return super.equals(obj);
    }
    com.macfu.dfs.namenode.rpc.model.HeartbeatResponse other = (com.macfu.dfs.namenode.rpc.model.HeartbeatResponse) obj;

    boolean result = true;
    result = result && (getStatus()
        == other.getStatus());
    result = result && getCommands()
        .equals(other.getCommands());
    return result;
  }

  @Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptorForType().hashCode();
    hash = (37 * hash) + STATUS_FIELD_NUMBER;
    hash = (53 * hash) + getStatus();
    hash = (37 * hash) + COMMANDS_FIELD_NUMBER;
    hash = (53 * hash) + getCommands().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.macfu.dfs.namenode.rpc.model.HeartbeatResponse prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @Override
  protected Builder newBuilderForType(
      BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code com.macfu.dfs.namenode.rpc.HeartbeatResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:com.macfu.dfs.namenode.rpc.HeartbeatResponse)
      com.macfu.dfs.namenode.rpc.model.HeartbeatResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.internal_static_com_macfu_dfs_namenode_rpc_HeartbeatResponse_descriptor;
    }

    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.internal_static_com_macfu_dfs_namenode_rpc_HeartbeatResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.class, com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.Builder.class);
    }

    // Construct using com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      status_ = 0;

      commands_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.internal_static_com_macfu_dfs_namenode_rpc_HeartbeatResponse_descriptor;
    }

    public com.macfu.dfs.namenode.rpc.model.HeartbeatResponse getDefaultInstanceForType() {
      return com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.getDefaultInstance();
    }

    public com.macfu.dfs.namenode.rpc.model.HeartbeatResponse build() {
      com.macfu.dfs.namenode.rpc.model.HeartbeatResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.macfu.dfs.namenode.rpc.model.HeartbeatResponse buildPartial() {
      com.macfu.dfs.namenode.rpc.model.HeartbeatResponse result = new com.macfu.dfs.namenode.rpc.model.HeartbeatResponse(this);
      result.status_ = status_;
      result.commands_ = commands_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.macfu.dfs.namenode.rpc.model.HeartbeatResponse) {
        return mergeFrom((com.macfu.dfs.namenode.rpc.model.HeartbeatResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.macfu.dfs.namenode.rpc.model.HeartbeatResponse other) {
      if (other == com.macfu.dfs.namenode.rpc.model.HeartbeatResponse.getDefaultInstance()) return this;
      if (other.getStatus() != 0) {
        setStatus(other.getStatus());
      }
      if (!other.getCommands().isEmpty()) {
        commands_ = other.commands_;
        onChanged();
      }
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.macfu.dfs.namenode.rpc.model.HeartbeatResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.macfu.dfs.namenode.rpc.model.HeartbeatResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int status_ ;
    /**
     * <code>optional int32 status = 1;</code>
     */
    public int getStatus() {
      return status_;
    }
    /**
     * <code>optional int32 status = 1;</code>
     */
    public Builder setStatus(int value) {
      
      status_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional int32 status = 1;</code>
     */
    public Builder clearStatus() {
      
      status_ = 0;
      onChanged();
      return this;
    }

    private Object commands_ = "";
    /**
     * <code>optional string commands = 2;</code>
     */
    public String getCommands() {
      Object ref = commands_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        commands_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }
    /**
     * <code>optional string commands = 2;</code>
     */
    public com.google.protobuf.ByteString
        getCommandsBytes() {
      Object ref = commands_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        commands_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>optional string commands = 2;</code>
     */
    public Builder setCommands(
        String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      commands_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional string commands = 2;</code>
     */
    public Builder clearCommands() {
      
      commands_ = getDefaultInstance().getCommands();
      onChanged();
      return this;
    }
    /**
     * <code>optional string commands = 2;</code>
     */
    public Builder setCommandsBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      commands_ = value;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:com.macfu.dfs.namenode.rpc.HeartbeatResponse)
  }

  // @@protoc_insertion_point(class_scope:com.macfu.dfs.namenode.rpc.HeartbeatResponse)
  private static final com.macfu.dfs.namenode.rpc.model.HeartbeatResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.macfu.dfs.namenode.rpc.model.HeartbeatResponse();
  }

  public static com.macfu.dfs.namenode.rpc.model.HeartbeatResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<HeartbeatResponse>
      PARSER = new com.google.protobuf.AbstractParser<HeartbeatResponse>() {
    public HeartbeatResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new HeartbeatResponse(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<HeartbeatResponse> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<HeartbeatResponse> getParserForType() {
    return PARSER;
  }

  public com.macfu.dfs.namenode.rpc.model.HeartbeatResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

