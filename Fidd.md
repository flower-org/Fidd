# Fidd  

Keeping your personal information off the Internet is really hard.  
Your stuff is scattered all over the place.  
Addresses, employment history, your family relationships and phone numbers.  
*Jack Rhysider.*


## 1. Fidd folder structure  
  
`Fiddler` - is an actor that owns one or more `Fidds`. The ownership is established by `Fiddler`'s key pair.  
Root folder holds one or more `Fidds` maintained by the `Fiddler`.  
  
Root folder can have an arbitrary name. It's possible to assign that name to `Fiddler`'s public key for convenience, but it's not required if other methods of communicating this key to subscribers is implemented for better security.  
  
Below is a high-level structure of `Fidd` folders.  
- It's recommended to use Random UUIDs for `Fidd` folder names to avoid any information leakage, and use System Object `FIDD_INFO` (described below) to store information about the `Fidd` in a properly encrypted way;  
- Message folders are named sequentially using a monotonically increasing sequence of positive numbers.  
  
```  
{FIDDLER_ROOT_FOLDER} /  
                -> {FIDD_ID (UUID)} /  
                              -> {MESSAGE_NUMBER (INDEX)} /  
                              -> {MESSAGE_NUMBER (INDEX)} /  
                              -> {MESSAGE_NUMBER (INDEX)} /  
                              ...  
                -> {FIDD2_ID (UUID)} /  
                -> {FIDD3_ID (UUID)} /  
                -> {FIDD4_ID (UUID)} /  
                ...  
```  
  
## 2. Messages and Objects  
  
While physically `Fidd`s consist of `Message`s, the view they're intended to provide is a list of `Object`s. `Object`s are folders, just like `Message`s are, but `Object`s are mutable and are defined by one or more `Message`s.  
  
`Messages` represent how `Object`'s folder changes over time, essentially being *diffs* between subsequent `Object` versions. The first `Message` in the object defines the initial `Object` state, and further `Message`s contain new files, binary and text *diffs* for the updated files. The document `structure_changes.yaml` describes structural changes, like files being moved, renamed and deleted. Those *diffs* are calculated on unencrypted files and then encrypted.  
  
A `Message` can completely overwrite an existing `Object`, and this type of update is also indicated in `structure_changes.yaml`. A possibility to completely overwrite an `Object` allows us to *Compact* old versions' `Message`s. We can do that if there were too many versions of an object created. In this scenario we loose a history of changes for the `Object`, but we can still serve the latest version of the `Object`, which is sufficient in most cases.  
  
All `Message`s in the `Fidd` are immutable. Compaction simply removes redundant `Messages` from the `Fidd` (the `Messages` that precede complete overwrite of an Object).  
*Compaction* is optional and can be selectively run on chosen `Objects`. If preserving change history for an `Object` is paramount, *Compaction* on that `Object` should be avoided.  
  
## 3. Message structure  
  
### 3.1. Message Folder structure  
  
Every message is a folder of the following structure:  
```  
{MESSAGE_NUMBER (INDEX)} /  
                         (metadata)  
                         -> .fidd /  
                                  -> keys.yaml  
                                  -> files.yaml  
                                  -> metadata.yaml  
                                  -> object_info.yaml  
                                  -> structure_changes.yaml  
  
                         (message data, arbitrary structure)  
                         -> {FILE_ID1}  
                         -> {FILE_ID2}  
                         -> {FILE_ID3}  
                         ...  
```  
  
### 3.2. files.yaml and keys.yaml  
  
- `keys.yaml` contains encryption information about keys required to unencrypt `files.yaml`, which includes:  
    - encryption key / per subscriber id; encrypted with subscriber's public key;  
      `keys.yaml` is not encrypted.  
  
- `files.yaml` contains information about attached files, that includes  
    - encryption key per file id;  
    - file path per file id;  
    - file offset and size in a metafile per file id;  
    - a record showing whether the file is a full file, or if it's a diff, and what diff algorithm was used.  
      `files.yaml` is encrypted normally.  
  
**N.B.** Paranoid measure could include encrypting all rows in files.yaml per subscriber with subscriber's key, in this case an occasional decryption of `keys.yaml` will not lead directly to decryption of the entire `Message`, without compromising at least one of subscribers' keys (at which point everything shared with that subscriber would be compromised).  
  
To further obfuscate the number of attached files, we store them as a single metafile, and in `files.yaml` we store the respective positions and sizes in the metafile.  
  
Also, to further minimize the possibilities of guessing the actual number of attached files from encrypted data, we can add a setting in the packer program to adjust the size of `files.yaml` itself to some size, say, the multiple of 10240 bytes (10k); or randomly add an empty or random postfix of 10k - 30k.  
  
### 3.3. metadata.yaml  
  
Metadata file is encrypted normally.  
Metadata file contains:  
- `Message` creation timestamp;  
- `object_id` for an `Object` that the `Message` belongs to.  
  
This file will be present in every `Message`.  
  
### 3.4. object_info.yaml  
  
Object info file is encrypted normally.  
Object info file contains:  
- `Object` name;  
- `Object` type (default "REGULAR");  
- optional ObjectSchemaType;  
- optional ObjectSchemaVersion, for imaginary future rich clients that can use those to set UI preferences, etc.  
  
This file will be present in the first `Message`, and in all further `Messages` that feature changes in `Object` it belongs to.  
  
### 3.5. structure_changes.yaml  
  
`structure_changes.yaml` file is encrypted normally.  
This file describes the following structural changes in the `Object`:  
- file path changed;  
- file deleted;  
- `Message` is not a delta update, but completely overwrites the Object.  
  
This file will will be present in all `Messages` that feature the aforementioned changes in file structure, other than new files added.  
New files added are not shown in this document, because it's sufficient to simply show them in `files.yaml`.  
  
## 4. System objects  
  
System objects are pre-defined objects with reserved types, with special meaning and structure.  
  
### 4.1. System object: type `FIDD_SUB`  
  
Information about subscriber ids.  
Id is also fixed to `FIDD_SUB`, singleton object.  
```  
subscriber_ids.yaml  
```  
This file is not encrypted.  
File format is as follows:  
`subscriber_id`: `encrypted_subscriber_id`  
- `subscriber_id` is the id for a subscriber that will be used in `keys.yaml`;  
- `encrypted_subscriber_id` is the same `subscriber_id` encrypted with subscriber's public key, acts as a signature.  
  Thus, it's possible to determine your `subscriber_id` by finding which `encrypted_subscriber_id` can be unencrypted by your private key and will match `subscriber_id`.  
  The reason for that is to avoid publishing subscriber's public keys in the open.  
  If the `Fiddler` has alternative means to communicate `subscriber_id` to subscribers, it's possible to not publish this list at all, to avoid leaking any information about the number of subscribers.  
  
### 4.2. System object: type `FIDD_SUB_PRIV`  
  
This is a list of subscribers' public keys.  
Id is also fixed to `FIDD_SUB_PRIV`, singleton object.  
```  
subscriber_public_keys.yaml  
```  
The file is encrypted with `Fiddler`'s public key, and acts as a backup for this information for the `Fiddler`.  
This object is optional and is not needed for subscribers to consume the `Fidd`.  
If the `Fiddler` has alternative means to store the list, it would be more secure to store this list elsewhere, to avoid exposing this information entirely.  
Note: with the `FIDD_SUB_PRIV` object we should supply *bogus keys* for all subscribers that are not the subscriber himself in `keys.yaml`, to avoid leaking information on `Object` type; due to the fact that `keys.yaml` is unencrypted.  
  
### 4.3. System object: type `FIDD_INFO`  
  
It contains information about the `Fidd` itself, like name, description, etc.  
Id is also fixed to `FIDD_INFO`, singleton object.  
  
```  
fidd_info.yaml  
```  
This file is encrypted normally.  
  
---  
  
P.S. The above format is intended to support portable serverless representation of `Fidd`s. A side-effect of that is that every action results in a new `Message` being created. Notably, adding a `subscriber` would mean that new `Message`s should be created for all `Object`s this `subscriber` will gain access to, which can be pretty taxing on `Fidd`s with large numbers of `Object`s.  
This *O(N)* new `Message`s effect, however, can be mitigated by providing a special `Object` with type `SUBSCRIBER_KEY_UPDATE` on new `subscriber` addition, providing the bulk key information for previous `Message`s, and reducing the overhead to *O(1)* new `Message`s.  
Note: with the `SUBSCRIBER_KEY_UPDATE` object we should supply *bogus keys* for all subscribers that are not the subscriber himself in `keys.yaml`, to avoid leaking information on `Object` type; due to the fact that `keys.yaml` is unencrypted.  
  
P.P.S. It's possible to imagine a server-based implementation of the same idea.  
This surely can be done, but having a server at all as a part of the picture might be undesirable in some scenarios, and in many other scenarios downloading new `Fidd` messages from the server and processing them in a quasi-offline mode would still be the preferable way to consume the content.  
That's why it's important to make sure that any possible hybrid implementations interacting with specialized servers shouldn't break the user's ability to create, update, retrieve, store, consume and distribute `Fidd`s in server-less way.  
  
## Appendix 1: summary of special encryption  
  
Below is the summary of files/objects that are not using standard end-to-end encryption.  
  
- Unencrypted:  
    - `.fidd/keys.yaml`  
    - `subscriber_ids.yaml` in `FIDD_SUB`  
  
- Always encrypted with `Fiddler`'s public key, *bogus keys* supplied for all the subscribers  
    - `subscriber_public_keys.yaml` in `FIDD_SUB_PRIV`  
  
- *Bogus keys* supplied for all subscribers except for the intended recipient:  
    - Objects with type `SUBSCRIBER_KEY_UPDATE`
 
- Even better, we can omit the public keys altogether.
  
## P.S.  
  
If `Message`s are immutable, why not zip them?  
And for best obfuscation, encrypt zip.  
MessageId can be in Zip's name, or it can be   
  
1. Common password  
Just zip file, it's a message. Common password for all messages.  


2. Variable password  
Computable password, based on MessageId, some common constant and algorithm (Variator module).  


3. Message in message  
A message with a zip file inside, which is an actual zipped message. AES-encrypted, as any Fidd message, with (1) or (2) on top if desired.  
   - With this approach we can theoretically hide the real order of messages, only exposing it in the inner layer, but it comes with a significant overhead of full index traversal to get the updates, and theoretically if the Fidd is public, an external observer still could periodically check the contents and keep track of the order in which the messages appeared.  
   Overall, while that's a super-paranoid measure, it's reasonable to implement, especially given that in naive FTP or other folder-based implementations a full index traversal will still be required to discover new messages.

## P.P.S.
A lot of components in this design intend to hide metadata.  
However, since we're operating on files, metadata can be leaked via external file attributes, like creation/change date.  
Ideally, all file-level metadata should also be erased, let's say dates set to beginning of Unix time, etc. At least for files immediately facing the external world.  
  * Maybe a special build of FTP server that hides metadata?
