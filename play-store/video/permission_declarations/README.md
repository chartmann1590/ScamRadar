# Permission Declaration Deliverables

## FOREGROUND_SERVICE_DATA_SYNC — foreground_service_data_sync_justification.mp4

Real on-device screen recording (Pixel 8 Pro) of the AI model download in
Settings → AI Model, showing the live 0%→100% progress notification, with
voiceover + burned-in captions explaining why the Data Sync foreground
service permission is used. Upload this file directly to the Play Console
"Foreground Service Permissions" declaration form.

## READ_MEDIA_IMAGES — description text

The permission was removed from the manifest (unused — both screenshot
pickers use `ActivityResultContracts.GetContent()` / `PickVisualMedia()`,
which require no media permission). If Play Console still shows the
"Describe your app's use of the READ_MEDIA_IMAGES permission" form before
it re-scans the latest AAB, paste this:

> Users tap "Scan screenshot" to pick one image of a suspicious message. The
> selected photo is analyzed on-device with OCR to detect scams. This is a
> one-time, user-initiated pick — the app never accesses photos in the
> background or browses the library.
