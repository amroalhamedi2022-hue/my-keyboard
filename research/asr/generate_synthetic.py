"""Create reproducible synthetic smoke corpus. Not human dialect evaluation."""
import json, pathlib, subprocess, sys, wave
out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
samples=[
 ("en01",[("en-us","Open the settings and write a short note")]),
 ("en02",[("en-us","The keyboard works without an internet connection")]),
 ("ar01",[("ar","هذه لوحة مفاتيح تعمل بدون اتصال بالإنترنت")]),
 ("ar02",[("ar","أريد أن أكتب رسالة قصيرة باللغة العربية")]),
 ("mix01",[("ar","افتح"),("en-us","Google Drive"),("ar","واحفظ الملف")]),
 ("mix02",[("ar","أريد تجربة"),("en-us","offline voice typing"),("ar","على الهاتف")]),
]
with (out/"manifest.jsonl").open("w",encoding="utf-8") as manifest:
    for ident, parts in samples:
        chunks=[]
        for i,(language,words) in enumerate(parts):
            raw=out/f"{ident}-{i}-raw.wav"; pcm=out/f"{ident}-{i}.wav"
            subprocess.run(["espeak-ng","-v",language,"-s","145","-w",str(raw),words],check=True)
            subprocess.run(["ffmpeg","-loglevel","error","-y","-i",str(raw),"-ar","16000","-ac","1","-c:a","pcm_s16le",str(pcm)],check=True)
            with wave.open(str(pcm),"rb") as w:
                assert (w.getnchannels(),w.getsampwidth(),w.getframerate())==(1,2,16000)
                chunks.append(w.readframes(w.getnframes()))
            raw.unlink();pcm.unlink()
        filename=ident+".wav"
        with wave.open(str(out/filename),"wb") as w:
            w.setparams((1,2,16000,0,"NONE","not compressed"));w.writeframes(b"\0"*6400+(b"\0"*4800).join(chunks)+b"\0"*6400)
        manifest.write(json.dumps({"id":ident,"audio":filename,"reference":" ".join(x[1] for x in parts),"dialect":"synthetic-"+ident.rstrip("0123456789"),"condition":"synthetic-espeak"},ensure_ascii=False)+"\n")
