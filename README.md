# About
This repository contains a sample app that shows how to encrypt the room database in an Android app.
The password used for encryption is generated on the first use
and is saved in the Android EncryptedSharedPreferences.
The app is programmed in kotlin. In java it should work in the same way.

The reason I created this repository was
that when I wanted to encrypt the database of an Android App,
I could only find outdated blog posts and tutorials using deprecated classes and methods.
So after I dug through the Android Documentation
I came up with a solution that I thought might be helpful to others.
So I am sharing this here in a sample App.

Feel free to open a pull request with improvements.

## Room Database Without Encryption
The branch `no-encryption` in this repository contains the source code of the app
without the room database being encrypted.

<img src="screenshots/screenshot_app.png" width="200"/>

By analyzing the database file with `hexdump` it is possible to verify that the database is not encrypted.
```shell
hexdump -C secret-database.db-wal
```

![](screenshots/screenshot_hex_not_encrypted.png)

## Encrypting the Room Database
### SQLCipher
[SQLCipher for Android](https://github.com/sqlcipher/android-database-sqlcipher)
adds encryption to a room database. The setup is simple:
The following dependency needs to be added to the app's `build.gradle` file:
```groovy
implementation 'net.zetetic:android-database-sqlcipher:4.5.0'
```
In the database class only two lines of code need to be added.
A SGLCipher `SupportFactory` is instantiated and passed to the `Room.databaseBuilder`:
```kotlin
val factory = SupportFactory(passphrase)
//Room.databaseBuilder( ... )
    .openHelperFactory(factory)
    //.build()
```
That is already it for the actual encryption.
Sadly SQLCipher does not handle the scenario
where a password is generated for encryption on the first use of the app
and stored encrypted in Android for future use.
The `SupportFactory` constructor needs a passphrase as an argument.
So a passphrase has to be generated.

### Generating a Passphrase
The class [javax.crypto.KeyGenerator](https://developer.android.com/reference/javax/crypto/KeyGenerator)
provides the means to generate Keys. A simple function to generate a passphrase would look like this:
```kotlin
const val ALGORITHM_AES = "AES"
const val KEY_SIZE = 256

private fun generatePassphrase(): ByteArray {
    val keyGenerator = KeyGenerator.getInstance(ALGORITHM_AES)
    keyGenerator.init(KEY_SIZE)
    return keyGenerator.generateKey().encoded
}
```
The generated passphrase can be used to create the SQLCipher `SupportFactory`.
But, of course, the passphrase also needs to be saved so the database can be decrypted in the future.

### Storing the Key in the EncryptedSharedPreferences
The passphrase can be stored in the
[EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences).
Those preferences can only be accessed by the app that created them and are encrypted by Android.

To do this a reference to the `EncryptedSharedPreferences` is needed. The classes
[MasterKey](https://developer.android.com/reference/androidx/security/crypto/MasterKey)
and [MasterKey.Builder](https://developer.android.com/reference/androidx/security/crypto/MasterKey.Builder)
and this specific [create method](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences#create(android.content.Context,%20java.lang.String,%20androidx.security.crypto.MasterKey,%20androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme,%20androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme))
of `EncryptedSharedPreferences` are, as the time of writing, the way to do this.
Other classes and methods have been deprecated.
To use these classes the following dependencies have to be added to the app's `build.gradle` file:
```groovy
implementation "androidx.security:security-crypto:1.0.0"
implementation "androidx.security:security-crypto-ktx:1.1.0-alpha03"
```

A function retrieving the reference to the `EncryptedSharedPreferences` could look like this:
```kotlin
const val SHARED_PREFS_NAME = "de.krbmr.encryptedroomdb.shared_prefs"

private fun getSharedPrefs(context: Context): SharedPreferences {
    val masterKey =
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    return EncryptedSharedPreferences.create(
        context,
        SHARED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```
Two more functions are needed. One to initialize the preference with a newly generated passphrase.
And one to retrieve the passphrase from the preference:
```kotlin
const val PREFS_KEY_PASSPHRASE = "PREFS_KEY_PASSPHRASE"

private fun initializePassphrase(context: Context): ByteArray {
    val passphrase = generatePassphrase()

    getSharedPrefs(context).edit(commit = true) {
        putString(PREFS_KEY_PASSPHRASE, passphrase.toString(Charsets.ISO_8859_1))
    }

    return passphrase
}

private fun getPassphrase(context: Context): ByteArray? {
    val passphraseString = getSharedPrefs(context)
        .getString(PREFS_KEY_PASSPHRASE, null)
    return passphraseString?.toByteArray(Charsets.ISO_8859_1)
}
```
Important to note is that the ByteArray is converted to a String before saving it to the preference.
The ISO-8859-1 encoding is used for that because it does not lose information on encoding and decoding.

Now it is possible to call the function `getPassphrase`
in the function `createDB` to get the passphrase.
When the app is used for the first time, null will be returned.
In this case the function `initializePassphrase` needs to be called.
```kotlin
val passphrase = getPassphrase(context) ?: initializePassphrase(context)
```

The finished kotlin database class can be found [here](app/src/main/java/de/krbmr/encryptedroomdb/database/SecretDatabase.kt).

By analyzing the database file with `hexdump` it is now possible to verify that the database is encrypted.
```shell
hexdump -C secret-database.db-wal
```
