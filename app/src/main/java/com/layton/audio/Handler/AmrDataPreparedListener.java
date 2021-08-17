package com.layton.audio.Handler;

/**
 * The the amr format data is prepared listener, you can send the amr data to server or save in local file
 */
public interface AmrDataPreparedListener {

    void prepared(byte[] data);
}
