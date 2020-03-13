package com.example.dz_3_2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "===MainActivity===";
    private  Bitmap[] bmpFiles;
    private  Map<String, Bitmap> mapBmp;
    private  static List<Component> listItem;
    private  static Component curItem;
    private int nmrlColor = Color.rgb(0xFF, 0xFF, 0xFF);
    private int slctColor = Color.rgb(0xE2, 0xA7, 0x6F);
    private static int curPos = -1; //т.е. 0 - то 1-ый будет выделен
    @SuppressLint("StaticFieldLeak")
    private  static View curView = null;
    private static ArrayAdapter<Component> adapter;
    private static File fileObj;
    private static File esCurDir;
    private static File esCurFile;
    private static AlertDialog dialogText;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
        esCurDir = getExternalStorageDirectory();   //сначала текущ. = корнев. директор.
        listItem = fillDirectory(esCurDir);       //список всех файлов внутри ее
        GridView gvFiles = findViewById(R.id.gvOne);
        //-------------------загруз. и установ. bmp в объекты Component
        this.getAssetManager();

        adapter = new ArrayAdapter<Component>(this, R.layout.gridview_item, R.id.tvItem, listItem){
            @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    Component component = this.getItem(position);

                    ImageView ivImg = (ImageView) view.findViewById(R.id.ivImg);
                    ivImg.setImageBitmap(component.bmp);

                    TextView tvTitle = view.findViewById(R.id.tvItem);
                    tvTitle.setText(component.title);

                    if(position == curPos){ //curPos изм. в onItemClick
                        view.setBackgroundColor(slctColor);
                        curView = view;
                    }else
                        view.setBackgroundColor(nmrlColor);

                    return view;
                }
            };

//            ((ViewGroup)gridView.getParent()).removeView(gridView);       //?? пригодиться
            gvFiles.setAdapter(adapter);
            //-----------обработ. выбора  файла (каталога)------------
            gvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    curItem = (Component) parent.getAdapter().getItem(position);
                    if (curPos != -1)   //нет выбора-снять затен.
                        curView.setBackgroundColor(nmrlColor);
                    curPos = position;
                    curView = view;
                    curView.setBackgroundColor(slctColor); //установ. затен. для выбранн. элем.
                    Toast.makeText(MainActivity.this, "Выбран: " + curItem.title, Toast.LENGTH_SHORT).show();
                    fileObj = (curItem.title.equals(".."))? (MainActivity.this.esCurDir.getParentFile()) :
                            (new File(MainActivity.this.esCurDir, curItem.title));
                }
            });
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED)||
                state.equals(Environment.MEDIA_MOUNTED_READ_ONLY));
    }
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED) ||
                state.equals(Environment.MEDIA_MOUNTED_READ_ONLY));
    }

    private ArrayList<Component> fillDirectory(File esMainDir) {
        ArrayList<Component> listFiles = new ArrayList<>();
        if (this.isExternalStorageReadable()) {
            File[] arrFiles = esMainDir.listFiles();
            if (arrFiles != null) {
                for (File file : arrFiles) {
                    if (file.isDirectory()) {
                        listFiles.add(new MyDirectory("[" + file.getName() + "]", "folder"));
                    }
                    else if(file.getName().contains("txt")||file.getName().contains("text") ||file.getName().contains("test")) {
                        listFiles.add(new MyFile(file.getName(), "txt"));
                    }
                    else {
                        listFiles.add(new MyFile(file.getName(), "other"));
                    }
                }
            } else
                Toast.makeText(this, "Каталог внешнего носителя пуст!", Toast.LENGTH_SHORT).show();
        }
        return listFiles;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private AssetManager getAssetManager() {    //установ bmp в items
        AssetManager assetManager = getAssets();
        //кол. img для для файлов не относ. к txt и folder
        int SIZE = 3;
        String[] imageNames = new String[SIZE +2];    //new String[]{"folder.png", "music.png", "text.png", "WMV.png"};
        try {
            imageNames = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream[] inputStreams = new InputStream[imageNames.length];
        bmpFiles = new Bitmap[imageNames.length];
        mapBmp = new HashMap<>();
        try{
            for(int i = 0; i < imageNames.length; i++){
                inputStreams[i] = assetManager.open(imageNames[i]);
                bmpFiles[i] = BitmapFactory.decodeStream(inputStreams[i]);
                String typeFile = imageNames[i].toLowerCase().substring(0,imageNames[i].indexOf("."));
                int finalI = i;
                if(typeFile.contains("folder")) {
                    listItem.stream().filter(item->item.typeFile.equals("folder")).forEach(item->item.bmp = bmpFiles[finalI]);
                    mapBmp.put("folder", bmpFiles[i]);
                }
                else if (typeFile.contains("doc") || typeFile.contains("txt")){
                    listItem.stream().filter(item->item.typeFile.equals("txt")).forEach(item->item.bmp = bmpFiles[finalI]);
                    mapBmp.put("txt", bmpFiles[i]);
                }
                else {
                    listItem.stream().filter(item->item.typeFile.equals("") ||item.typeFile == null).forEach(item->item.bmp = bmpFiles[finalI]);
                    mapBmp.put("other", bmpFiles[i]);
                }
                inputStreams[i].close();
            }
        }catch (Exception e){
            Log.d(TAG, "Ошибка загрузки изображений: "+e.getMessage());
        }
        return assetManager;
    }
    //----------------------------------------------------------------------------------------------
    //меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
//обработка меню

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_open_file: {
                if (curItem == null) {
                    Toast.makeText(this, "Не выбран файл/каталог!", Toast.LENGTH_SHORT).show();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                //попытка выйти в папку -/0
                if (esCurDir.getName().equals(String.valueOf(0)) && curItem.title.equals("..")) {
                    Toast.makeText(this, "Это запретная область!", Toast.LENGTH_SHORT).show();
                    curView.setBackgroundColor(nmrlColor);
                    break;
                }
                if (isExternalStorageWritable()) {
                    File esMainDir = getExternalStorageDirectory();
                    TextView tvOne = findViewById(R.id.tvActivity);
                    tvOne.setText(esMainDir.getAbsolutePath());
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Widget_AbsListView);    //нужен при откр. файла
                    curItem.title = curItem.title.replaceAll("[\\[\\]]", "");
                    //при клике по [..] или вошел, но не выбран файл, то наверх - в родит. каталог
                    fileObj = (curItem.title.equals("..") || curItem.title.equals(esCurDir.getName())) ?
                            (MainActivity.this.esCurDir.getParentFile()) : (new File(MainActivity.this.esCurDir, curItem.title));
                    //отображение содержимого-------------------------------------------------------
                    //--------------------если директория ---------
                    if (fileObj.isDirectory()) {
                        //при клике по катал. вывести его cодержим.
                        MainActivity.this.adapter.clear();
                        List<Component> listFiles = MainActivity.this.fillDirectory(fileObj);//в Components еще надо IMG !
                        if (listFiles.size() > 0) {        //+IMG
                            listFiles.stream().filter(f -> f.typeFile.equals("folder")).forEach(f -> f.bmp = mapBmp.get("folder"));
                            listFiles.stream().filter(f -> f.typeFile.equals("txt")).forEach(f -> f.bmp = mapBmp.get("txt"));
                            listFiles.stream().filter(f -> f.typeFile.equals("other")).forEach(f -> f.bmp = mapBmp.get("other"));
                        }
                        //для глав.корн. катал. не добавл.[..]
                        //т.е. в отображ. списка файлов- будут файлы + ссылка на корн. кат.[..]
                        if (fileObj.compareTo(getExternalStorageDirectory()) != 0) {
                            MainActivity.this.adapter.add(new MyDirectory(mapBmp.get("folder"), "[..]", "folder"));
                        }
                        MainActivity.this.adapter.addAll(listFiles);
                        MainActivity.this.esCurDir = fileObj;
                        curPos = -1;
                        //---------------------если файл--------------------------------
                    } else if (fileObj.isFile()) {
                        try {
                            LineNumberReader LR = new LineNumberReader(new FileReader(fileObj));
                            String S = "";
                            while (true) {
                                String z = LR.readLine();
                                if (z == null) break;
                                S += z + "\n";
                            }
                            LR.close();
                            //при откр. файла - Диалог редакт. текста
                            builder.setTitle(fileObj.getName());
                            LayoutInflater inflater = getLayoutInflater();
                            View viewDialog = inflater.inflate(R.layout.edit_text_dialog, null, false);

                            EditText etEditFile = (EditText) viewDialog.findViewById(R.id.etEditFile);
                            etEditFile.setText(S);
                            builder.setView(viewDialog);
                            dialogText = builder.create();
                            dialogText.show();

                            Log.d(TAG, "S: " + S);    //текст файла
                            // ----- Запомним путь к файлу -----------------------------------------
                            MainActivity.this.esCurFile = fileObj;
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Ошибка открытия файла: \n" +
                                    e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(MainActivity.this, fileObj.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }

                    tvOne.setText(esCurDir.getAbsolutePath());  //в шапке абс. путь к выбр. файлу
                } else
                    Log.d(MainActivity.TAG, "Внешний	носитель	не	готов!");
            }
                break;

            case R.id.action_create_file: {
                if (isExternalStorageWritable()) {
                    if (esCurDir.getName().equals("0") && fileObj == null) {
                        Toast.makeText(this, "Директория не выбрана!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    //взять имя нов. файла (из будущ. Диалог №2) и созд. 1) Component, 2) File
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault);
                    builder.setTitle("Введите название файла");
                    LayoutInflater inflater = this.getLayoutInflater();
                    View view = inflater.inflate(R.layout.dialog_name_file, null, false);
                    builder.setView(view);
                    builder.setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //--	Чтение	содержимого	текстов.полей	Диалогового	окна	------
                            EditText editText = (EditText) view.findViewById(R.id.etFileName);
                            String nameNewFile = editText.getText().toString();
                            if (!nameNewFile.contains(".") ||
                                    !nameNewFile.substring(nameNewFile.indexOf("."), nameNewFile.length() - 1).equals("txt")) {
                                nameNewFile += ".txt";
                            }
                            if (nameNewFile.isEmpty()) {//если не введено имя ФАЙЛА-создать случ. назв.
                                nameNewFile = "noname" + ((int) (Math.random() * 100)) + ".txt";
                            }
                            //созд. файл
                            File fileName = new File(esCurDir, nameNewFile);
                            //..и параллель. легенду файла
                            Component component = new MyFile(nameNewFile, "txt");
                            component.bmp = mapBmp.get("txt");
                            adapter.add(component);
                            adapter.notifyDataSetChanged();
                            try {
                                if (fileName.createNewFile())
                                    Toast.makeText(MainActivity.this, "Файл создан успешно: \n" + nameNewFile, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Log.e(TAG, "Ошибка создания файла, c.305: " + e.getMessage());
                            }
                        }
                    });
                    builder.setNegativeButton("Отменить", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else
                    Toast.makeText(this, "Внешний носитель не готов", Toast.LENGTH_SHORT).show();
            }
                break;

            case R.id.action_create_folder: {
                Log.d(TAG, "esCurDir: " + esCurDir.getName());
                if (!isExternalStorageWritable()) {
                    Toast.makeText(this, "Внешний носитель не готов", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (esCurDir.getName().equals("0") && fileObj == null) {
                    Toast.makeText(this, "Директория не выбрана!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                //взять имя нов. файла (из будущ. Диалог №2) и созд. 1) Component, 2) File
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault);
                builder.setTitle("Введите название файла");
                LayoutInflater inflater = this.getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_name_file, null, false);
                builder.setView(view);
                builder.setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    //--	Чтение	содержимого	текстов.полей	Диалогового	окна	------
                        EditText  editText = (EditText) view.findViewById(R.id.etFileName);
                        String nameNewFolder = editText.getText().toString();
                        nameNewFolder = nameNewFolder.replaceAll("[\\[\\]]", "");   //теперь самодель. папки будут удаляться
                        if(nameNewFolder.isEmpty()) //если не введено имя ФАЙЛА-создать случ. назв.
                            nameNewFolder = "noname" + ((int) (Math.random() * 100));
                        //созд. каталог
                        File folder = new File(esCurDir, nameNewFolder);
                        Component component = new MyDirectory(mapBmp.get("folder"), nameNewFolder, "txt");
                        adapter.add(component);
                        adapter.notifyDataSetChanged();
                        try {
                            if(folder.mkdir())
                                Toast.makeText(MainActivity.this, "Каталог создан успешно: \n"+ nameNewFolder, Toast.LENGTH_SHORT).show();
                        } catch (Exception e){
                            Log.e(TAG, "Ошибка создания каталога: "+e.getMessage());
                        }
                    }
                });
                builder.setNegativeButton("Отмена", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
                break;
            //---------------удален. только файлов ???-----------------------------------------------------------
            case R.id.action_remove_file:
                if(fileObj == null){
                    Toast.makeText(this, "Директория/файл не выбран(а)!", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (isExternalStorageWritable()) {
                    if(fileObj.isFile()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault);
                        builder.setTitle("Вы действительно хотите удалить этот файл: ");
                        builder.setMessage(curItem.title + "?");
                        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (fileObj.delete())
                                    Log.d(TAG, "Файл " + curItem.title + " удален");
                                adapter.remove(curItem);
                                adapter.notifyDataSetChanged();
                            }
                        });
                        builder.setNegativeButton("Нет", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault);
                        builder.setTitle("Вы действительно хотите удалить этот каталог: ");
                        builder.setMessage(curItem.title + "?");
                        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(delDir(fileObj))
                                    Log.d(TAG, "Каталог " + curItem.title + " удален");
                                else Log.d(TAG, "Ошибка удаления каталога " + curItem.title);
                                adapter.remove(curItem);
                                adapter.notifyDataSetChanged();
                            }
                        });
                        builder.setNegativeButton("Нет", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean delDir(File dir){
        boolean flag = false;
        File reFile = null;
        String reName="";
        if(dir.getName().contains("[]")||dir.getName().contains("]")) {
            reName = dir.getName().replaceAll("[\\[\\]]", "");
            reFile = new File(reName);
            if(dir.renameTo(reFile)) Log.d(TAG, "каталог перименован");;
        }
        File[] files = dir.listFiles();
        if(files == null || files.length == 0) {    //если папка пустая
            if(dir.delete()) flag=true;
            else {
                flag = false;
                Log.d(TAG, "Каталог " + curItem.title + " НЕ удален");
            }
        }
        else {                                           //если нет
            for(int i = 0; i < files.length; i++){
                File file = files[i];
                if(file.isFile()){      //если содержим. -  файл
                    if(file.delete()) flag = true;
                    else {
                        flag = false;
                    }
                }
                else delDir(file);      //если папка - в рекурсию
            }
        }
        return flag;
    }

    //-------------------------Редактир. текста файла-------------------------------------------------------------------
    public void btnEditSaveClick(View view) {
        if(esCurFile == null) {
            Toast.makeText(this, "Для сохранения необходимо предварительно выбрать и открыть файл",
                    Toast.LENGTH_SHORT).show();        }
        else {
            if (isExternalStorageWritable()) {
                EditText etEditFile = dialogText.findViewById(R.id.etEditFile);
                String content = etEditFile.getText().toString();
                Log.d(TAG, "content: "+content);

                //пересозд. файл и запись в него содержим.
                try {
                    FileWriter f = new FileWriter(esCurFile);
                    f.write(content + "\r\n");
                    f.flush();
                    f.close();
                    Toast.makeText(this, "Файл пересоздан успешно: \n" + esCurFile.getAbsolutePath(),
                            Toast.LENGTH_SHORT).show();
                } catch (IOException ioe) {
                    Toast.makeText(this, "Ошибка записи в пересозданный файл: \n" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Ошибка записи в файл: " + ioe.getMessage());
                }
            } else
                Toast.makeText(this, "Внешний носитель не готов", Toast.LENGTH_SHORT).show();
        }
    }
}
