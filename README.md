# udp-audio

A application for encoding the audio data to "amr-wb" format which recorded by AudioRecord with format of ENCODING_PCM_16BIT, decode the "amr-wb" audio data to "pcm" 
for play with AudioTrack.

There is two method provided for handle the encoded audio data:
1. PlayDataListener.java is able to play the audio data which is recorded by self, this aim to verify the accuracy of the audio encoding, decoding and playback.
2. RemoteDataListener.java is able to send the audio data to remote server, recommend to use udp. now you should accomplish it by yourself.
