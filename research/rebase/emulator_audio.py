"""Replay a consented 16 kHz mono WAV into a local Android emulator microphone."""
import wave


def replay_wav(path, port):
    # The CI generates the module from its installed emulator's public gRPC schema.
    import grpc
    import emulator_controller_pb2 as proto

    with wave.open(str(path), "rb") as audio:
        assert (audio.getframerate(), audio.getnchannels(), audio.getsampwidth()) == (16000, 1, 2)
        assert 0 < audio.getnframes() <= 30 * 16000, "audio must fit one dictation session"

        def packets():
            audio_format = proto.AudioFormat(samplingRate=16000, format=proto.AudioFormat.AUD_FMT_S16)
            yield proto.AudioPacket(format=audio_format, audio=bytes(640))
            while (pcm := audio.readframes(320)):
                yield proto.AudioPacket(audio=pcm)
            for _ in range(85):
                yield proto.AudioPacket(audio=bytes(640))

        with grpc.insecure_channel(f"127.0.0.1:{port}") as channel:
            inject_audio = channel.stream_unary(
                "/android.emulation.control.EmulatorController/injectAudio",
                request_serializer=proto.AudioPacket.SerializeToString,
            )
            inject_audio(packets(), timeout=55, wait_for_ready=True)
