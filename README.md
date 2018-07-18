# trafficapp_0523
參考網址：

定位GoogleMap 
- https://www.androidtutorialpoint.com/intermediate/android-map-app-showing-current-location-android/


計算兩點距離 
- https://www.mytrendin.com/draw-route-two-specific-locations-google-maps-android/


Retrofit教學 
- https://www.androidtutorialpoint.com/networking/retrofit-android-tutorial/
- https://windsuzu.github.io/learn-android-retrofit2/


Amazon
登錄- https://signin.aws.amazon.com/signin?redirect_uri=https%3A%2F%2Fap-northeast-1.console.aws.amazon.com%2Felasticbeanstalk%2Fhome%3Fregion%3Dap-northeast-1%26state%3DhashArgs%2523%252Fenvironment%252Fdashboard%253FapplicationName%253DTraffic%2526environmentId%253De-fkermumwdu%26isauthcode%3Dtrue&client_id=arn%3Aaws%3Aiam%3A%3A015428540659%3Auser%2Felasticbeanstalk&forceMobileApp=0
RDS- https://ap-northeast-1.console.aws.amazon.com/rds/home?region=ap-northeast-1#dbinstance:id=aa3dnfj9u7n51z
Elastic Beanstalk- https://ap-northeast-1.console.aws.amazon.com/elasticbeanstalk/home?region=ap-northeast-1#/environment/dashboard?applicationName=Traffic&environmentId=e-fkermumwdu

Elastic Beanstalk上傳php檔案：

先將index.php放進一個資料夾
打開終端機
到你設置資料夾的底下, 
// 先打, ll //是查詢目前位置所有資料夾 
// 再打, cd <<資料夾名稱>>  //到這個資料夾裡

打下以下指令, 最好一次一個打
pip install awsebcli    //下載amazon的cli
eb init --interactive   //輸入一些資料

//以下為輸入資料要選的
Select a default region
1) us-east-1 : US East (N. Virginia)
2) us-west-1 : US West (N. California)
3) us-west-2 : US West (Oregon)
4) eu-west-1 : EU (Ireland)
5) eu-central-1 : EU (Frankfurt)
6) ap-south-1 : Asia Pacific (Mumbai)
7) ap-southeast-1 : Asia Pacific (Singapore)
8) ap-southeast-2 : Asia Pacific (Sydney)
9) ap-northeast-1 : Asia Pacific (Tokyo)
10) ap-northeast-2 : Asia Pacific (Seoul)
11) sa-east-1 : South America (Sao Paulo)
12) cn-north-1 : China (Beijing)
13) cn-northwest-1 : China (Ningxia)
14) us-east-2 : US East (Ohio)
15) ca-central-1 : Canada (Central)
16) eu-west-2 : EU (London)
17) eu-west-3 : EU (Paris)
(default is 3): 9

Select an application to use
1) Traffic
2) [ Create new Application ]
(default is 2): 1

It appears you are using PHP. Is this correct?
(Y/n): y

Select a platform version.
1) PHP 7.1
2) PHP 7.0
3) PHP 5.6
4) PHP 5.5
5) PHP 5.4
6) PHP 5.3
(default is 1): 1
Cannot setup CodeCommit because there is no Source Control setup, continuing with initialization
Do you want to set up SSH for your instances?
(Y/n): y

Select a keypair.
1) my-first-instance
2) traffic
3) [ Create new KeyPair ]
(default is 2): 2

接下來打以下指令
eb deploy
 
 //按enter後跑出以下 為已成功！
 Creating application version archive "app-180709_233233".
Uploading Traffic/app-180709_233233.zip to S3. This may take a while.
Upload Complete.
INFO: Environment update is starting.
INFO: Deploying new version to instance(s).
INFO: New application version was deployed to running EC2 instances.
INFO: Environment update completed successfully.

