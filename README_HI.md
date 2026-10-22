# Echo Offline — Native Android starter project

यह Android Studio के लिए Kotlin starter project है।

## अभी क्या करता है?
- पहली बार खुलने पर Android का Music/Audio permission prompt दिखाता है।
- अनुमति मिलने पर MediaStore से फ़ोन में indexed music tracks स्कैन करता है।
- गानों की सूची दिखाता है और Play बटन से गाना चलाता है।
- ऐप खुला रहने पर MediaStore में बदलाव दिखने पर library refresh करता है।
- एक ही MediaStore ID वाली entries को दोबारा नहीं दिखाता।

## सीमाएँ
- यह तैयार APK नहीं है; यह Android Studio source project है।
- ऐप बंद होने पर हर नए डाउनलोड को तुरंत पकड़ने की गारंटी नहीं है। Reliable background detection के लिए अलग native background design और testing चाहिए।
- Android MediaStore में केवल वही audio दिखेगा जिसे सिस्टम ने index किया है।
- यह starter app अभी playlists, liked songs, notification controls और background playback service शामिल नहीं करता।

## Build कैसे करें?
1. Android Studio इंस्टॉल करें।
2. Repository को clone/download करके project folder खोलें।
3. Android SDK 35 इंस्टॉल करें और Gradle sync होने दें।
4. USB debugging के साथ Android phone जोड़ें और Run दबाएँ।
5. APK बनाने के लिए: **Build > Build Bundle(s) / APK(s) > Build APK(s)**।

पहली बार चलाते समय केवल अपनी music files तक पहुँचने की अनुमति दें। ऐप किसी server पर गाने upload नहीं करता।
