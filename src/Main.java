import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
// import javafx.geometry.Point3D; // Не используется, можно удалить
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

// Пожалуйста, убедитесь, что у вас есть класс Finch.java или соответствующий JAR
// Если нет, и вы хотите запустить без робота, раскомментируйте заглушку Finch в конце файла.
// import com.birdbraintechnologies.Finch; // Пример, если Finch в пакете

public class Main extends Application {
    private double[] accelerationArray = new double[3];

    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS); // Динамический поворот по Y (рыскание)
    private Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    // Эти поля по умолчанию инициализируются нулями, что корректно для старта
    private double currentAngleX = 0;
    private double currentAngleY = 0; // Для динамического рыскания, сейчас не используется активно
    private double currentAngleZ = 0;

    private final double smoothingFactor = 0.15;
    private final double sensitivityThreshold = 0.05;

    private final double sensitivityX = 70.0; // Для Pitch (тангаж модели, управляется Y робота)
    private final double sensitivityY = 70.0; // Для Yaw (рыскание модели, пока не используется активно)
    private final double sensitivityZ = 70.0; // Для Roll (крен модели, управляется X робота)

    private final double MAX_ROTATION_DEGREES = 90.0;

    private PrintWriter dataLogger;
    private long startTime;

    private double accelOffsetX_robot = 0;
    private double accelOffsetY_robot = 0;

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Finch myFinch = null;
        try {
            myFinch = new Finch(); // Используй свой класс Finch
        } catch (Exception e) {
            System.err.println("Не удалось инициализировать Finch: " + e.getMessage());
            System.err.println("Убедитесь, что робот подключен и библиотеки Finch доступны.");
            // Можно здесь показать диалоговое окно с ошибкой и завершить приложение,
            // или продолжить с заглушкой (если это предусмотрено).
            // Platform.exit(); // Завершить приложение JavaFX
            // return; // Завершить метод start
            // Пока что просто выведем ошибку и попробуем продолжить (может упасть позже)
        }


        // --- КАЛИБРОВКА НУЛЯ ---
        double[] initialAccel = {0.0, 0.0, -1.0}; // Значения по умолчанию, если робот не доступен
        if (myFinch != null) { // Проверяем, что myFinch был успешно инициализирован
            initialAccel = myFinch.getAcceleration();
            if (initialAccel != null && initialAccel.length >= 2) {
                accelOffsetX_robot = initialAccel[0];
                accelOffsetY_robot = initialAccel[1];
            } else {
                System.err.println("Ошибка калибровки: не удалось получить начальные данные акселерометра или массив слишком короткий!");
                accelOffsetX_robot = 0;
                accelOffsetY_robot = 0;
            }
        } else {
            System.err.println("Finch не инициализирован, калибровка невозможна. Используются смещения по умолчанию (0).");
            accelOffsetX_robot = 0;
            accelOffsetY_robot = 0;
        }

        System.out.printf("Calibration: Initial Raw Accel from Finch = [%.2f, %.2f, %.2f]%n",
                initialAccel[0],
                initialAccel.length > 1 ? initialAccel[1] : 0.0, // Безопасный доступ
                initialAccel.length > 2 ? initialAccel[2] : 0.0); // Безопасный доступ
        System.out.printf("Calibration: Offset for Robot X (accel[0]) set to: %.2f%n", accelOffsetX_robot);
        System.out.printf("Calibration: Offset for Robot Y (accel[1]) set to: %.2f%n", accelOffsetY_robot);


        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            dataLogger = new PrintWriter(new FileWriter("accel_data_" + timeStamp + ".csv"));
            dataLogger.println("Time,RawAccelX,RawAccelY,RawAccelZ,CalibRobotX,CalibRobotY,TargetModelPitch(X),TargetModelYaw(Y),TargetModelRoll(Z),CurrentModelPitch(X),CurrentModelYaw(Y),CurrentModelRoll(Z),ClampedModelPitch(X),ClampedModelYaw(Y),ClampedModelRoll(Z)");
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("Ошибка создания файла логов: " + e.getMessage());
        }

        Group root = new Group();
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight(), true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.DARKSLATEGRAY);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-700);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(45);
        scene.setCamera(camera);

        Group model = loadCustomModel();
        model.getTransforms().addAll(rotateY, rotateX, rotateZ);

        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-400);
        keyLight.setTranslateY(-300);
        keyLight.setTranslateZ(-500);

        AmbientLight ambientLight = new AmbientLight(Color.rgb(120, 120, 120));
        root.getChildren().addAll(model, keyLight, ambientLight);

        primaryStage.setTitle("3D Robot Control (Finch) - Calibrated - Corrected Orientation & Pitch");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setFullScreen(true);
        primaryStage.show();

        Finch finalMyFinch = myFinch; // Для использования в лямбде таймера

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (finalMyFinch != null) { // Проверяем, что робот доступен
                    accelerationArray = finalMyFinch.getAcceleration();
                    if (accelerationArray == null || accelerationArray.length < 3) {
                        // Если данные не пришли или некорректны, используем предыдущие или нули
                        // Это предотвратит NullPointerException ниже
                        // Можно добавить логирование этой ситуации
                        System.err.println("Warning: Failed to get acceleration data or data is incomplete.");
                        accelerationArray = (accelerationArray == null || accelerationArray.length < 3) ? new double[]{accelOffsetX_robot, accelOffsetY_robot, -1.0} : accelerationArray; // Используем калибровочные нули как базу, если есть проблема
                    }
                } else {
                    // Если робот не был инициализирован, симулируем стояние на месте
                    // или какое-то дефолтное поведение.
                    // Чтобы не было NullPointerException, accelerationArray должен быть инициализирован.
                    // Это уже сделано на уровне класса: private double[] accelerationArray = new double[3];
                    // Мы можем присвоить ему значения, соответствующие откалиброванному нулю.
                    accelerationArray[0] = accelOffsetX_robot;
                    accelerationArray[1] = accelOffsetY_robot;
                    accelerationArray[2] = -1.0; // Примерное значение для Z в покое
                }

                long currentTime = System.currentTimeMillis() - startTime;

                double calibratedRobotX = accelerationArray[0] - accelOffsetX_robot;
                double calibratedRobotY = accelerationArray[1] - accelOffsetY_robot;

                // --- СОПОСТАВЛЕНИЕ ДАННЫХ ---
                // ИЗМЕНЕНИЕ ЗДЕСЬ: убран минус перед calibratedRobotY для коррекции тангажа
                double targetModelAngleX = calibratedRobotY * sensitivityX;
                double targetModelAngleY = 0.0; // Yaw пока отключен
                double targetModelAngleZ = calibratedRobotX * sensitivityZ;

                currentAngleX = applySmoothing(currentAngleX, targetModelAngleX);
                currentAngleY = applySmoothing(currentAngleY, targetModelAngleY);
                currentAngleZ = applySmoothing(currentAngleZ, targetModelAngleZ);

                double clampedAngleX = clamp(currentAngleX, -MAX_ROTATION_DEGREES, MAX_ROTATION_DEGREES);
                double clampedAngleY = clamp(currentAngleY, -MAX_ROTATION_DEGREES, MAX_ROTATION_DEGREES);
                double clampedAngleZ = clamp(currentAngleZ, -MAX_ROTATION_DEGREES, MAX_ROTATION_DEGREES);

                rotateX.setAngle(clampedAngleX);
                rotateY.setAngle(clampedAngleY);
                rotateZ.setAngle(clampedAngleZ);

                if (dataLogger != null) {
                    dataLogger.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                            currentTime,
                            accelerationArray[0], accelerationArray[1], accelerationArray[2],
                            calibratedRobotX, calibratedRobotY,
                            targetModelAngleX, targetModelAngleY, targetModelAngleZ,
                            currentAngleX, currentAngleY, currentAngleZ,
                            clampedAngleX, clampedAngleY, clampedAngleZ);
                    dataLogger.flush();
                }

                System.out.printf("T:%dms RobRaw:[%.1f,%.1f,%.1f] RobCal:[%.1f,%.1f] MdlTgt:[P:%.1f,Y:%.1f,R:%.1f] MdlCrnt:[P:%.1f,Y:%.1f,R:%.1f] MdlClmp:[P:%.1f,Y:%.1f,R:%.1f]%n",
                        currentTime,
                        accelerationArray[0], accelerationArray[1], accelerationArray[2],
                        calibratedRobotX, calibratedRobotY,
                        targetModelAngleX, targetModelAngleY, targetModelAngleZ,
                        currentAngleX, currentAngleY, currentAngleZ,
                        clampedAngleX, clampedAngleY, clampedAngleZ);
            }
        };
        timer.start();

        primaryStage.setOnCloseRequest(event -> {
            if (dataLogger != null) {
                dataLogger.close();
            }
            if (finalMyFinch != null) { // Проверяем, что робот был инициализирован перед отключением
                finalMyFinch.disconnect();
            }
            timer.stop();
            System.out.println("Application closed.");
        });
    }

    private Group loadCustomModel() {
        Group modelGroup = new Group();
        Box body = new Box(150, 100, 200);
        PhongMaterial bodyMaterial = new PhongMaterial(Color.LIGHTSKYBLUE);
        body.setMaterial(bodyMaterial);
        Box nose = new Box(30, 30, 50);
        nose.setMaterial(new PhongMaterial(Color.ORANGERED));
        nose.setTranslateZ(body.getDepth() / 2 + nose.getDepth() / 2);
        Box wing = new Box(250, 10, 20);
        wing.setMaterial(new PhongMaterial(Color.LIGHTGREEN));
        modelGroup.getChildren().addAll(body, nose, wing);
        modelGroup.setTranslateZ(150);

        // Применяем начальный поворот к группе модели, чтобы она "смотрела" в нужную сторону.
        Rotate initialOrientationY = new Rotate(180, Rotate.Y_AXIS);
        modelGroup.getTransforms().add(initialOrientationY);

        return modelGroup;
    }

    private Group loadGirlModel() {
        try {
            Group modelGroup = new Group();
            Box box = new Box(200, 200, 200);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.LIGHTBLUE);
            material.setSpecularColor(Color.WHITE);
            material.setSpecularPower(32.0);
            box.setMaterial(material);
            modelGroup.getChildren().add(box);
            modelGroup.setTranslateY(0);
            modelGroup.setTranslateX(0);
            modelGroup.setTranslateZ(200);
            return modelGroup;
        } catch (Exception e) {
            System.err.println("Ошибка создания модели: " + e.getMessage());
            e.printStackTrace();
            return createFallbackModel();
        }
    }

    private Group createFallbackModel() {
        Box box = new Box(100, 100, 100);
        box.setMaterial(new PhongMaterial(Color.PINK));
        return new Group(box);
    }


    private double applySmoothing(double current, double target) {
        if (Math.abs(target - current) < sensitivityThreshold) {
            return target;
        }
        return lerp(current, target, smoothingFactor);
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
}

// --- Заглушка для класса Finch ---
// Если у вас нет библиотеки Finch или робот не подключен,
// вы можете раскомментировать этот класс для тестирования логики JavaFX.
// Убедитесь, что закомментирован любой другой import для Finch.

/*
class Finch {
    private java.util.Random random = new java.util.Random();

    public Finch() {
        System.out.println("Finch STUB initialized");
    }

    public double[] getAcceleration() {
        // Имитация данных акселерометра
        // [0] = X (лево/право), [1] = Y (вперед/назад), [2] = Z (вверх/вниз)
        // Когда робот стоит ровно на колесах, Z ~ -1.0g
        // Наклон носом вниз: Y становится отрицательным
        // Наклон носом вверх: Y становится положительным
        // Наклон на правый бок: X становится отрицательным
        // Наклон на левый бок: X становится положительным

        // Пример: легкое дрожание в горизонтальном положении
        // double x = (random.nextDouble() - 0.5) * 0.1; // небольшой шум для X
        // double y = (random.nextDouble() - 0.5) * 0.1; // небольшой шум для Y

        // Для тестирования конкретных наклонов:
        // Ровно:
        // double x = 0.0; double y = 0.0;
        // Носом немного вниз:
        // double x = 0.0; double y = -0.3;
        // Носом немного вверх:
        // double x = 0.0; double y = 0.3;
        // На правый бок:
        // double x = -0.3; double y = 0.0;
        // На левый бок:
        // double x = 0.3; double y = 0.0;

        // Возвращаем случайные данные для демонстрации
        double x = (random.nextDouble() * 2.0 - 1.0) * 0.5; // от -0.5 до 0.5
        double y = (random.nextDouble() * 2.0 - 1.0) * 0.5; // от -0.5 до 0.5
        double z = -0.8 - random.nextDouble() * 0.4; // от -0.8 до -1.2

        // System.out.printf("Stub Finch Accel: [%.2f, %.2f, %.2f]%n", x,y,z);
        return new double[]{x, y, z};
    }

    public void disconnect() {
        System.out.println("Finch STUB disconnected");
    }

    // Другие методы Finch, если они вызываются (например, setLED, move, etc.)
    // должны быть также добавлены сюда как заглушки, если ваша программа их использует.
}
*/