CAPSTONE PROJECT - IMAGE LABELLING ANDROID APPLICATION... IN PROGRESS

The goal of this project is to create an application that allows users to gather and label images from a mobile device, in order to create datasets that can be used to train machine learning models. 


1 Methodology

The application addresses the growing need for efficient image dataset compilation by developing a user-friendly Android application to assist with image annotation. This innovative mobile tool allows users to effortlessly gather, label, and organize images directly from an Android device. By leveraging the power of a mobile device, the application enables individuals to actively participate in the advancement of machine learning models by making annotated datasets more accessible. 

<img width="1085" alt="Screen Shot 2024-02-08 at 6 40 52 PM" src="https://github.com/makaela12/496projIMGLBLAPP/assets/114378861/3098d908-9bbc-4edd-9939-6f9f205c0754">

2 Application

2.1 Hardware

The application supports Android devices, including smartphones and tablets. Android was chosen as the target platform due to its widespread popularity and accessibility among users. The Android operating system offers a diverse range of devices with varying specifications, ensuring that the application can cater to a broad user base. Android devices contain embedded cameras that allow users to capture and import images directly into the application. These devices also contain the processing power and storage capacity required to handle image annotation and dataset generation, ensuring a seamless user experience. This choice of hardware maximizes the applications reach and usability, making it accessible to a broad audience of potential users that wish to develop annotated datasets for machine learning models.  

2.2.2 Software

The application was constructed with Android Studio Hedgehog | 2023.1.1 Patch 1 in Java programming language. This integrated development environment (IDE) was chosen as it is the official IDE for Android application development. Java was the chosen programming language for this application, because it has been the primary language for Android development for numerous years. Although Koltin is often preferred to Java now for Android development, having previous experience with Java led to this choice of language. In addition, the Android Emulator, Android SDK and the Android Virtual Device (AVD) Manager were installed. The Android 13.0 (Tiramisu) SDK was installed to provide a collection of libraries and tools to use in development, such as a debugger and compiler, as well as documentation and code libraries to provide shortcuts to repetitive code sequences. The Android Emulator is a virtual device that was used to test the application, because a physical device was inaccessible during development. The Pixel XL API 30 virtual device was used because it is the most efficient emulator to run on macOS. The AVD Manager was used to create and manage the virtual device when testing the application, allowing for the simulation of an Android smartphone’s characteristics while running the application.
The application is flexible, providing users with the option to either choose to create a new project or continue working on existing projects. With the option to upload an image or take a new image directly within the app, users can define object locations within images by creating bounding boxes and assigning appropriate labels. This application supports You Only Look Once (YOLO) and Common Objects in Context (COCO) labeling formats. YOLO was chosen for its simplicity in data management, where it consolidates object annotations into a single text file for each corresponding image. Because this format is straightforward, YOLO also ensures compatibility with popular machine learning models. This format includes essential annotation information, such as object labels, center coordinates, height, and width within bounding boxes. COCO was chosen because of its popularity in the computer vision field, where its structure has been standardized and widely accepted for representing annotations. Having a format with this consistency is crucial for interoperability, enabling the app to have a seamless integration with other tools and software that support COCO format. This format stores annotations in a JSON file, which contains information about the image, as well as a list of annotated objects. Annotation information includes the object labels, bounding box coordinates and area of the bounding box.To store the image annotations and project details, SQLite serves as the local database in this application. Given that this application primarily targets users that work on small to medium sized image datasets, SQLite will be a suitable choice. SQLite is known for its efficiency and reliability, making it an ideal choice for a mobile application. Using a local database provides a lightweight, embedded solution that does not require a separate server or network connectivity. With SQLite, users can work on projects while offline, which provides a more versatile and responsive experience.

 
2.3 Wireframe
<img width="1107" alt="Screen Shot 2024-02-08 at 6 41 16 PM" src="https://github.com/makaela12/496projIMGLBLAPP/assets/114378861/8045b751-9a05-40eb-9955-d3d69f3e550b">
<img width="1095" alt="Screen Shot 2024-02-08 at 6 41 36 PM" src="https://github.com/makaela12/496projIMGLBLAPP/assets/114378861/55e9b1e5-8339-4bd2-afc8-83f0346237aa">

The app’s homepage provides users with the option to either create a new project or view a list of existing projects. When creating a new project, a user can predefine the labels required to annotate images. If a user would like to access a preexisting project, a list of all past projects are displayed to the user, with the option to search for a project by its project name. A user can delete a project by clicking on holding on the project name in the list to be removed. Selecting a project from the list prompts a display page to show the user a gallery of all the images currently annotated within the project, coupled with the number of annotations for each image. Glide, an image loading and caching library, was used to load the images to ensure smooth scrolling [7]. A user can scroll down the page to view all images within a project. To modify an image, a user can remove or add bounding boxes by selecting a desired image from the gallery. From this page, a user can also choose to re-crop the image. To delete an image from the project, a user can select and hold down on an image in the gallery to be removed. To modify the project name or its labels, a user can select the menu icon in the top right corner and select the option to edit the project. To add a new image to a project, a user can select the option to either import an image from the device's camera roll, or take a new photo directly within the app by clicking a corresponding icon at the bottom of the screen. After selecting an image, the user can crop the image with a window aspect ratio and adjust it to the desired position on the image. Android Image Cropper, an image cropping library for android, was utilized to implement the cropping functionality [8]. After cropping the image, a user can select a label name and draw a bounding box on the image. If a user would like to delete the last bounding box, this can be done by clicking on the remove icon. A user can draw multiple bounding boxes on an image, and use different label names for each bounding box. If a user would like to re-crop the image, this can be done by clicking on the re-crop icon. The project can be exported by selecting the export icon display on the bottom right of the image gallery. Users can choose to format the project in either YOLO or COCO labeling format, and choose a platform to share the files with.



References 
[1]   J. Nelson, “How to Label Images for Computer Vision Models.” Roboflow. https://blog.roboflow.com/tips-for-how-to-label-images/ (accessed Oct. 2, 2023).

[2]   G. D. Maayan, “Complete Guide to Image Labeling for Machine Learning.” Dataversity. https://www.dataversity.net/complete-guide-to-image-labeling-for-machine-learning/ (accessed Oct. 2, 2023).

[3]   K. Lindman, J. F. Rose, M. Lindvall, C. Lundström, and D. Treanor, "Annotations, ontologies, and whole slide images – Development of an annotated ontology-driven whole slide image library of normal and abnormal human tissue," J. Pathol. Inform., vol. 10, no. 1, Jul. 2019. Accessed: Oct. 4, 2023. doi: 10.4103/jpi.jpi_81_18. [Online]. Available: https://www.sciencedirect.com/science/article/pii/S2153353922003856.

[4]   F. Loor, V. Gil-Costa, and M. Marin, "Processing Collections of Geo-Referenced Images for Natural Disasters," Journal of Computer Science & Technology (JCS&T), vol. 18, no. 3, pp. 193-202, Dec. 2018. Accessed Oct. 4, 2023. doi: 10.24215/16666038.18.e22. [Online]. Available: https://library.macewan.ca/full-record/iih/133614742. 

[5]   C. Sager, C. Janiesch, and P. Zschech, "A survey of image labeling for computer vision applications," Journal of Business Analytics, vol. 4, no. 2, pp. 91-110, Apr. 2021. Accessed Oct. 4, 2023. doi: 10.1080/2573234X.2021.1908861. [Online]. Available: https://www.tandfonline.com/doi/full/10.1080/2573234X.2021.1908861.

[6]   A. Arunachalam, V. Ravi, V. Acharya, and T. D. Pham, "Toward Data-Model-Agnostic Autonomous Machine-Generated Data Labeling and Annotation Platform: COVID-19 Autoannotation Use Case," IEEE Transactions on Engineering Management, vol. 70, no. 8, pp. 2695-2706, Aug. 2023. Accessed Oct. 4, 2023. doi: 10.1109/TEM.2021.3094544. [Online]. Available: https://library.macewan.ca/full-record/edseee/edseee.9500060.

[7]  S. Judd, “Glide.” Accessed: Jan 25, 2024. [Online]. Available: https://github.com/bumptech/glide?tab=readme-ov-file 

[8]  A. Teplitzki, “Android Image Cropper.” Accessed: Jan 25, 2024. [Online]. Available: https://github.com/ArthurHub/Android-Image-Cropper/tree/master.

