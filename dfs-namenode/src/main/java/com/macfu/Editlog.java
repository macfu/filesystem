package com.macfu;

import com.alibaba.fastjson.JSONObject;

/**
 * 代表一条edit log
 */
public class Editlog {

    private long txid;
    private String content;

    public Editlog(long txid, String content) {
        this.txid = txid;

        JSONObject jsonObject = JSONObject.parseObject(content);
        jsonObject.put("txid", txid);
        this.content = jsonObject.toJSONString();
    }

    public long getTxid() {
        return txid;
    }

    public void setTxid(long txid) {
        this.txid = txid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Editlog{" +
                "txid=" + txid +
                ", content='" + content + '\'' +
                '}';
    }
}
