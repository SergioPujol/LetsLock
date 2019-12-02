package es.upv.gnd.letslock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.core.Query;

import java.util.ArrayList;

import es.upv.gnd.letslock.Fragments.PersonasFragment;
import es.upv.gnd.letslock.Fragments.InicioFragment;
import es.upv.gnd.letslock.Fragments.NotificacionesFragment;
import es.upv.gnd.letslock.Fragments.PlanoFragment;
import es.upv.gnd.letslock.bbdd.AdaptadorUsuarios;
import es.upv.gnd.letslock.bbdd.AdaptadorUsuariosFirestoreUI;
import es.upv.gnd.letslock.bbdd.Usuario;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Fragment> fragments;
    private BottomNavigationView navigation;

    private RecyclerView recyclerView;
    public AdaptadorUsuarios adaptador;
    public static AdaptadorUsuariosFirestoreUI adaptador2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {

            //Establecemos la página de inicio como la primera
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentUtilizado, new InicioFragment()).commit();
            fragments = new ArrayList<>();
            fragments.add(new InicioFragment());
        }

        //Creamos el eventListener que nos permite cambiar de fragment
        navigation = findViewById(R.id.BotomNavigationView);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Inicializamos la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    //Inicializa el menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //Accion que realiza cuando haga click en uno de los elementos del menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.nav_cerrarsesion:

                AuthUI.getInstance().signOut(MainActivity.this).addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent i = new Intent(MainActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        MainActivity.this.finish();
                    }
                });
                break;

            case R.id.nav_perfil:

                Intent intent = new Intent(this, TabsActivity.class);
                startActivity(intent);
                break;


            case R.id.nav_ajustes:

                Intent intent2 = new Intent(this, PreferenciasActivity.class);
                startActivity(intent2);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //Menú inferior, cambia de fragment cada vez que toque un icono
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            Fragment fragSeleccionado= null;

            switch (item.getItemId()) {

                case R.id.menu_inferior_inicio:

                    fragSeleccionado = new InicioFragment();
                    break;
                case R.id.menu_inferior_plano:

                    fragSeleccionado = new PlanoFragment();
                    break;
                case R.id.menu_inferior_notificaciones:

                    fragSeleccionado = new NotificacionesFragment();
                    break;
                case R.id.menu_inferior_personas:

                    fragSeleccionado = new PersonasFragment();
                    break;
            }

            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.fragmentUtilizado, fragSeleccionado).addToBackStack(null).commit();
            fragments.add(fragSeleccionado);
            return true;
        }
    };

    @Override
    public void onBackPressed() {

        fragments.remove(fragments.size()-1);

        //Cuando hace click en volver hacia atras checkea el item anterior y establece ese fragment
        if(!fragments.isEmpty()){

            Fragment fragmentAnterior= fragments.get(fragments.size()-1);

            if (fragmentAnterior instanceof InicioFragment) {

                Log.i("aa", String.valueOf(navigation.getMenu().findItem(R.id.menu_inferior_inicio).setChecked(true)));

            } else if (fragmentAnterior instanceof PlanoFragment) {

                Log.i("aa", String.valueOf(navigation.getMenu().findItem(R.id.menu_inferior_plano).setChecked(true)));

            } else if (fragmentAnterior instanceof NotificacionesFragment) {

                Log.i("aa", String.valueOf(navigation.getMenu().findItem(R.id.menu_inferior_notificaciones).setChecked(true)));

            } else if (fragmentAnterior instanceof PersonasFragment) {

                Log.i("aa", String.valueOf(navigation.getMenu().findItem(R.id.menu_inferior_personas).setChecked(true)));
            }

            super.onBackPressed();

        //Si no hay item anterior cierra la aplicación
        }else {

            MainActivity.this.finishAffinity();
        }
    }
}
