package com.example.serpumar.androidthings_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

//import com.chaos.view.PinView;
import com.example.serpumar.comun.Datos;
import com.example.serpumar.comun.Imagen;
import com.goodiebag.pinview.Pinview;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import es.upv.gnd.letslock.androidthings.R;

import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends AppCompatActivity {


    public Datos dato;
    private ArduinoUart uart = new ArduinoUart("UART0", 9600);
    final String TAG = "Respuesta";
    private static final int INTERVALO = 30000; // Intervalo (ms)
    private static final int INTERVALO_TAG = 1000; // Intervalo (ms)
    private Handler handler = new Handler(); // Handler
    public Button upload;

    public String tag;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Timbre
    private ButtonInputDriver mButtonInputDriver;


    private DoorbellCamera mCamera;
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private Handler temporizadorHandler = new Handler();

    String pinPropietario = "";

    Pinview pinview;
    private int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.i("Prueba", "Lista de UART disponibles: " + ArduinoUart.disponibles());

        update();

        pinview = findViewById(R.id.pinview);

        pinview.setPinViewEventListener(new Pinview.PinViewEventListener() {
            @Override
            public void onDataEntered(Pinview pinview, boolean fromUser) {
                Toast.makeText(MainActivity.this, pinview.getValue(), Toast.LENGTH_SHORT).show();
                comprobarPin(pinview.getValue());
            }
        });


        //Timbre

        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        initPIO();

        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        /*try {
            handler.post(runnableRFID);
            handler.post(runnable); // 3. Llamamos al handler
            Log.d("Prueba","Llega aqui?");
        } catch (Exception e) {
            Log.e(TAG, "Error en PeripheralIO API", e);
        }*/

        //enterPin();
        Button btnSolicitarPin = findViewById(R.id.btnSolicitarCodigo);
        btnSolicitarPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AccesoActivity.class);
                startActivity(i);
            }
        });

    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                String distancia = getDistancia();
                Log.d(TAG, "Recibido de Arduino de DISTANCIA: " + distancia);
                String presencia = getPresencia();
                Log.d(TAG, "Recibido de Arduino de PRESENCIA: " + presencia);
                dato = new Datos(distancia, presencia, tag);
                Log.d("Datos recibidos: ", dato.getDistancia() + dato.getPresencia() + dato.getTag());
                handler.postDelayed(runnable, INTERVALO);
                // 5. Programamos siguiente llamada dentro de INTERVALO ms
            } catch (Exception e) {
                Log.e(TAG, "Error al recibir Datos", e);
            }
        }
    };

    private Runnable runnableRFID = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Llega a RFID??");
                tag = getEtiquetasRFID();
                if (!tag.equals("")) {
                    enviarDatosFirestoreTag(tag);
                }
                handler.postDelayed(runnableRFID, INTERVALO_TAG);
                // 5. Programamos siguiente llamada dentro de INTERVALO ms
            } catch (Exception e) {
                Log.e(TAG, "Error al recibir Datos", e);
            }
        }
    };


    public String getDistancia() {
        uart.escribir("D");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        String distancia = uart.leer();
        return distancia;
    }

    public String getPresencia() {
        uart.escribir("P");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        String presencia = uart.leer();
        return presencia;
    }

    public String getEtiquetasRFID() {
        uart.escribir("G");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        String tag = uart.leer();
        Log.d(TAG, "Recibido de Arduino de RFID: " + tag);
        return tag;
    }

    public void abrirPuerta() {
        uart.escribir("A");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        //String s = uart.leer();
        //Log.d(TAG, "Recibido de Arduino de AbrirPuerta: " + s);
    }

    public void cerrarPuerta() {
        uart.escribir("C");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        //String s = uart.leer();
        //Log.d(TAG, "Recibido de Arduino de CerrarPuerta: " + s);
    }

    public void abrirPuertaRFID() {

        String tag = getEtiquetasRFID();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error en sleep()", e);
        }
        Log.d(TAG, "EtiquetaRFID: " + tag);
        if (tag.equals(" 04 2A C1 5A 51 59 80 ")) { //Etiqueta 3
            abrirPuerta();
        } else {
            Log.d(TAG, "Este usuario no existe");
        }
    }

    public void enviarDatosFirestore(Datos dato) {
        db.collection("Datos").document("Datos").update("distancia", dato.getDistancia());
        db.collection("Datos").document("Datos").update("presencia", dato.getPresencia());

        //db.collection("Datos").document("Datos").update("tag","12345");
    }

    private void initPIO() {
        try {
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    com.google.android.things.contrib.driver.button.Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();
        } catch (IOException e) {
            mButtonInputDriver = null;
            Log.w("gpio pins", "Could not open GPIO pins", e);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d("btn pressed", "button pressed");
            mCamera.takePicture();
            return true;
        }


        return super.onKeyUp(keyCode, event);
    }


    static void registrarImagen(String titulo, String url) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Imagen imagen = new Imagen(titulo, url);
        db.collection("imagenes_timbre").document().set(imagen);
    }

    /* private Runnable tomaFoto = new Runnable() {
         @Override public void run() {
             mCamera.takePicture();
             temporizadorHandler.postDelayed(tomaFoto, 60 * 1000);
             //Programamos siguiente llamada dentro de 60 segundos
         }
     };*/
    private ImageReader.OnImageAvailableListener
            mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();
                    onPictureTaken(imageBytes);
                }
            };

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            String nombreFichero = UUID.randomUUID().toString();
            subirBytes(imageBytes, "imagenes_timbre/" + nombreFichero);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            /*runOnUiThread(new Runnable() {
                @Override public void run() {
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);
                }
            });*/
        }
    }

    private void subirBytes(final byte[] bytes, String referencia) {
        final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference ref = storageRef.child(referencia);
        UploadTask uploadTask = ref.putBytes(bytes);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull
                                          Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Log.e("Almacenamiento", "URL: " + downloadUri.toString());
                    registrarImagen("Subida por R.P.", downloadUri.toString());
                } else {
                    Log.e("Almacenamiento", "ERROR: subiendo bytes");
                }
            }
        });
    }


    public void enviarDatosFirestoreTag(String tag) {
        db.collection("Datos").document("Datos").update("tag", tag);
    }

    private void update() {
        db.collection("Datos").document("Puerta").addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                //user = documentSnapshot.toObject(User.class);
                //user.setUid(mAuth.getUid());
                if (documentSnapshot.getBoolean("puerta")) {
                    abrirPuerta();
                    Log.d("Puerta", "Puerta abierta");
                } else if (!documentSnapshot.getBoolean("puerta")) {
                    cerrarPuerta();
                    Log.d("Puerta", "Puerta cerrada");

                }
            }
        });
    }

    public void comprobarPin(final String pin) {
        db.collection("Datos").document("Puerta").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                //Si consigue leer en Firestore
                if (task.isSuccessful()) {

                    String codigo = task.getResult().getString("pin");


                    Log.d("Pin", codigo);
                    if (pin.equals(codigo)) {
                        db.collection("Datos").document("Puerta").update("puerta", true);
                        Toast.makeText(MainActivity.this, "PIN CORRECTO, DESBLOQUEANDO PUERTA", Toast.LENGTH_SHORT).show();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Log.w(TAG, "Error en sleep()", e);
                        }
                        db.collection("Datos").document("Puerta").update("puerta", false);
                        count = 0;
                    } else {
                        if (count == 3) {
                            Toast.makeText(MainActivity.this, "PIN INCORRECTO, intentos: " + count, Toast.LENGTH_SHORT).show();
                            count = 0;
                        } else {
                            count++;
                            Toast.makeText(MainActivity.this, "PIN INCORRECTO, intentos: " + count, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

    }

}
