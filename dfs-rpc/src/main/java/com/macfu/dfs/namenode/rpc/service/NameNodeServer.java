// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: NameNodeRpcServer.proto

package com.macfu.dfs.namenode.rpc.service;

public final class NameNodeServer {
  private NameNodeServer() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\027NameNodeRpcServer.proto\022\031com.macfu.dfs." +
      "namenode.rpc\032\026NameNodeRpcModel.proto2\262\014\n" +
      "\017NameNodeService\022c\n\010register\022*.com.macfu." +
      "dfs.namenode.rpc.RegisterRequest\032+.com.z" +
      "hss.dfs.namenode.rpc.RegisterResponse\022f\n" +
      "\theartbeat\022+.com.macfu.dfs.namenode.rpc.H" +
      "eartbeatRequest\032,.com.macfu.dfs.namenode." +
      "rpc.HeartbeatResponse\022Z\n\005mkdir\022\'.com.zhs" +
      "s.dfs.namenode.rpc.MkdirRequest\032(.com.zh" +
      "ss.dfs.namenode.rpc.MkdirResponse\022c\n\010shu",
      "tdown\022*.com.macfu.dfs.namenode.rpc.Shutdo" +
      "wnRequest\032+.com.macfu.dfs.namenode.rpc.Sh" +
      "utdownResponse\022r\n\rfetchEditsLog\022/.com.zh" +
      "ss.dfs.namenode.rpc.FetchEditsLogRequest" +
      "\0320.com.macfu.dfs.namenode.rpc.FetchEditsL" +
      "ogResponse\022\207\001\n\024updateCheckpointTxid\0226.co" +
      "m.macfu.dfs.namenode.rpc.UpdateCheckpoint" +
      "TxidRequest\0327.com.macfu.dfs.namenode.rpc." +
      "UpdateCheckpointTxidResponse\022e\n\006create\022," +
      ".com.macfu.dfs.namenode.rpc.CreateFileReq",
      "uest\032-.com.macfu.dfs.namenode.rpc.CreateF" +
      "ileResponse\022~\n\021allocateDataNodes\0223.com.z" +
      "hss.dfs.namenode.rpc.AllocateDataNodesRe" +
      "quest\0324.com.macfu.dfs.namenode.rpc.Alloca" +
      "teDataNodesResponse\022\212\001\n\025informReplicaRec" +
      "eived\0227.com.macfu.dfs.namenode.rpc.Inform" +
      "ReplicaReceivedRequest\0328.com.macfu.dfs.na" +
      "menode.rpc.InformReplicaReceivedResponse" +
      "\022\226\001\n\031reportCompleteStorageInfo\022;.com.zhs" +
      "s.dfs.namenode.rpc.ReportCompleteStorage",
      "InfoRequest\032<.com.macfu.dfs.namenode.rpc." +
      "ReportCompleteStorageInfoResponse\022\231\001\n\032ch" +
      "ooseDataNodeFromReplicas\022<.com.macfu.dfs." +
      "namenode.rpc.ChooseDataNodeFromReplicasR" +
      "equest\032=.com.macfu.dfs.namenode.rpc.Choos" +
      "eDataNodeFromReplicasResponse\022\201\001\n\022reallo" +
      "cateDataNode\0224.com.macfu.dfs.namenode.rpc" +
      ".ReallocateDataNodeRequest\0325.com.macfu.df" +
      "s.namenode.rpc.ReallocateDataNodeRespons" +
      "e\022f\n\trebalance\022+.com.macfu.dfs.namenode.r",
      "pc.RebalanceRequest\032,.com.macfu.dfs.namen" +
      "ode.rpc.RebalanceResponseB5\n!com.macfu.df" +
      "s.namenode.rpc.serviceB\016NameNodeServerP\001" +
      "b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.getDescriptor(),
        }, assigner);
    com.macfu.dfs.namenode.rpc.model.NameNodeRpcModel.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
