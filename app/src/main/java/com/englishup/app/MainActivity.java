package com.englishup.app;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private static final int MIC_PERMISSION = 201, SPEECH_REQUEST = 202;
  private final String[] english = {
    "Hello, my name is Alex.", "I am sixteen years old.", "I live in Brazil.",
    "I like learning English.", "I study every day.", "What is your name?",
    "Where are you from?", "I went to school yesterday.",
    "I would like some water, please.", "Could you speak more slowly?",
    "I have been studying English.", "If I had more time, I would practice more."
  };
  private final String[] portuguese = {
    "Olá, meu nome é Alex.", "Eu tenho dezesseis anos.", "Eu moro no Brasil.",
    "Eu gosto de aprender inglês.", "Eu estudo todos os dias.", "Qual é o seu nome?",
    "De onde você é?", "Eu fui à escola ontem.",
    "Eu gostaria de um pouco de água, por favor.", "Você poderia falar mais devagar?",
    "Eu venho estudando inglês.", "Se eu tivesse mais tempo, praticaria mais."
  };
  private final String[] levels = {"A1","A1","A1","A1","A1","A1","A1","A2","A2","B1","B1","B2"};
  private TextToSpeech tts;
  private TextView status, sentence, translation, counter;
  private int index;
  private boolean speechReady = false;

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    index = Math.max(0, Math.min(english.length-1, getPreferences(0).getInt("lesson",0)));
    tts = new TextToSpeech(this, code -> {
      speechReady = code == TextToSpeech.SUCCESS;
      if(speechReady) tts.setLanguage(Locale.US);
    });
    render();
  }
  private TextView label(String value, int size) {
    TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
    t.setTextColor(Color.rgb(24,36,59)); t.setPadding(0,14,0,14); return t;
  }
  private Button button(String text, LinearLayout root, View.OnClickListener action) {
    Button b = new Button(this); b.setText(text); b.setAllCaps(false);
    root.addView(b, new LinearLayout.LayoutParams(-1,-2)); b.setOnClickListener(action); return b;
  }
  private void render() {
    ScrollView scroll = new ScrollView(this);
    LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(32,42,32,36); box.setBackgroundColor(Color.rgb(248,250,255));
    scroll.addView(box);
    TextView heading = label("EnglishUp 🎓",30); heading.setTypeface(null,Typeface.BOLD); box.addView(heading);
    box.addView(label("Inglês prático • Fala e escuta • Sem jogos",15));
    counter = label("Lição "+(index+1)+" de "+english.length+"  •  Nível "+levels[index],16); box.addView(counter);
    sentence=label(english[index],26); sentence.setTypeface(null,Typeface.BOLD); box.addView(sentence);
    translation=label(portuguese[index],18); box.addView(translation);
    status=label("Ouça a frase e tente repeti-la em inglês.",16); box.addView(status);
    button("🔊 Ouvir pronúncia",box,v->speak());
    button("🎤 Responder falando",box,v->startSpeech());
    button("➡️ Próxima lição",box,v->{index=(index+1)%english.length;save();render();});
    button("⬅️ Lição anterior",box,v->{index=(index+english.length-1)%english.length;save();render();});
    box.addView(label("A análise de fala compara palavras reconhecidas, não mede fonética nem substitui um professor de IA. O reconhecimento de voz pode precisar de serviços de fala instalados e conexão.",13));
    setContentView(scroll);
  }
  private void save(){getPreferences(0).edit().putInt("lesson",index).apply();}
  private void speak() {
    if(!speechReady){status.setText("Áudio ainda indisponível neste aparelho.");return;}
    tts.speak(english[index],TextToSpeech.QUEUE_FLUSH,null,"lesson");
  }
  private void startSpeech(){
    if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_PERMISSION);return;
    }
    try {
      Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");
      intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Fale em inglês");
      startActivityForResult(intent,SPEECH_REQUEST);
    } catch(Exception e) {
      status.setText("Reconhecimento de voz não disponível. Verifique os serviços de fala do Android.");
    }
  }
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
    super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    if(requestCode==MIC_PERMISSION){
      if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startSpeech();
      else status.setText("Microfone negado. Ative a permissão nas configurações do Android.");
    }
  }
  private String normalize(String s) {
    return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]","").replaceAll("\\s+"," ").trim();
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode==SPEECH_REQUEST){
      if(resultCode!=RESULT_OK || data==null){status.setText("Não consegui ouvir. Tente novamente.");return;}
      ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
      if(results==null || results.isEmpty()){status.setText("Não reconheci a fala.");return;}
      String heard=results.get(0), expected=english[index];
      if(normalize(heard).equals(normalize(expected))) status.setText("✅ Frase reconhecida corretamente!\nVocê disse: "+heard);
      else {
        Set<String> words=new HashSet<>(Arrays.asList(normalize(heard).split(" ")));
        ArrayList<String> missing=new ArrayList<>();
        for(String word:normalize(expected).split(" ")) if(!words.contains(word)) missing.add(word);
        status.setText("Você disse: "+heard+"\nFrase-alvo: "+expected+
          "\nPalavras para praticar: "+(missing.isEmpty()?"revise a ordem das palavras":android.text.TextUtils.join(", ",missing))+
          "\nTente ouvir e repetir.");
      }
    }
  }
  @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
