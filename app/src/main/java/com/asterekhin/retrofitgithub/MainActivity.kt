package com.asterekhin.retrofitgithub

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.asterekhin.retrofitgithub.CredentialsDialog.ICredentialsDialogListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


class MainActivity : AppCompatActivity(), ICredentialsDialogListener {
    var githubAPI: GithubAPI? = null
    var username: String? = null
    var password: String? = null
    var repositoriesSpinner: Spinner? = null
    var issuesSpinner: Spinner? = null
    var commentEditText: EditText? = null
    var sendButton: Button? = null
    var loadReposButtons: Button? = null
    private val compositeDisposable: CompositeDisposable? = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById<View>(R.id.my_toolbar) as Toolbar
        setSupportActionBar(toolbar)
        sendButton = findViewById<View>(R.id.send_comment_button) as Button
        repositoriesSpinner = findViewById<View>(R.id.repositories_spinner) as Spinner
        repositoriesSpinner!!.isEnabled = false
        repositoriesSpinner!!.adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("No repositories available")
        )
        repositoriesSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                if (parent.selectedItem is GithubRepo) {
                    val githubRepo: GithubRepo = parent.selectedItem as GithubRepo
                    compositeDisposable!!.add(
                        githubAPI!!.getIssues(githubRepo.owner, githubRepo.name)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith<DisposableSingleObserver<List<GithubIssue>>>(
                                issuesObserver
                            )
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        issuesSpinner = findViewById<View>(R.id.issues_spinner) as Spinner
        issuesSpinner!!.isEnabled = false
        issuesSpinner!!.adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Please select repository")
        )
        commentEditText = findViewById<View>(R.id.comment_edittext) as EditText
        loadReposButtons = findViewById<View>(R.id.loadRepos_button) as Button
        createGithubAPI()
    }

    override fun onStop() {
        super.onStop()
        if (compositeDisposable != null && !compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_credentials -> {
                showCredentialsDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCredentialsDialog() {
        val dialog: CredentialsDialog = CredentialsDialog()
        val arguments: Bundle = Bundle()
        arguments.putString("username", username)
        arguments.putString("password", password)
        dialog.arguments = arguments
        dialog.show(supportFragmentManager, "credentialsDialog")
    }

    private fun createGithubAPI() {
        val gson: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .registerTypeAdapter(GithubRepo::class.java, GithubRepoDeserializer())
            .create()
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest: Request = chain.request()
                val builder: Request.Builder = originalRequest.newBuilder().header(
                    "Authorization",
                    Credentials.basic(username, password)
                )
                val newRequest: Request = builder.build()
                chain.proceed(newRequest)
            }.build()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(GithubAPI.ENDPOINT)
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        githubAPI = retrofit.create(GithubAPI::class.java)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.loadRepos_button -> compositeDisposable!!.add(
                githubAPI!!.getRepos()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(repositoriesObserver)
            )

            R.id.send_comment_button -> {
                val newComment: String = commentEditText!!.text.toString()
                if (!newComment.isEmpty()) {
                    val selectedItem: GithubIssue = issuesSpinner!!.selectedItem as GithubIssue
                    selectedItem.comment = newComment
                    compositeDisposable!!.add(
                        githubAPI!!.postComment(selectedItem.comments_url, selectedItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(commentObserver)
                    )
                } else {
                    Toast.makeText(this@MainActivity, "Please enter a comment", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private val repositoriesObserver: DisposableSingleObserver<List<GithubRepo>>
        get() = object : DisposableSingleObserver<List<GithubRepo>>() {
            override fun onSuccess(value: List<GithubRepo>) {
                if (value.isNotEmpty()) {
                    val spinnerAdapter: ArrayAdapter<GithubRepo?> = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        value
                    )
                    repositoriesSpinner!!.adapter = spinnerAdapter
                    repositoriesSpinner!!.isEnabled = true
                } else {
                    val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        arrayOf("User has no repositories")
                    )
                    repositoriesSpinner!!.adapter = spinnerAdapter
                    repositoriesSpinner!!.isEnabled = false
                }
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Can not load repositories", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    private val issuesObserver: DisposableSingleObserver<List<GithubIssue>>
        get() {
            return object : DisposableSingleObserver<List<GithubIssue>>() {
                override fun onSuccess(value: List<GithubIssue>) {
                    if (!value.isEmpty()) {
                        val spinnerAdapter: ArrayAdapter<GithubIssue?> = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            value
                        )
                        issuesSpinner!!.isEnabled = true
                        commentEditText!!.isEnabled = true
                        sendButton!!.isEnabled = true
                        issuesSpinner!!.adapter = spinnerAdapter
                    } else {
                        val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            arrayOf("Repository has no issues")
                        )
                        issuesSpinner!!.isEnabled = false
                        commentEditText!!.isEnabled = false
                        sendButton!!.isEnabled = false
                        issuesSpinner!!.adapter = spinnerAdapter
                    }
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Can not load issues", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    private val commentObserver: DisposableSingleObserver<ResponseBody>
        get() {
            return object : DisposableSingleObserver<ResponseBody>() {
                override fun onSuccess(value: ResponseBody) {
                    commentEditText!!.setText("")
                    Toast.makeText(this@MainActivity, "Comment created", Toast.LENGTH_LONG).show()
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Can not create comment", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onDialogPositiveClick(username: String, password: String) {
        this.username = username
        this.password = password
        loadReposButtons!!.isEnabled = true
    }

}