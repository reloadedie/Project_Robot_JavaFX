import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.image.Image;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

public class Main extends Application {
    private double[] accelerationArray = new double[3];
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private double currentAngleX = 0;
    private double currentAngleY = 0;
    private double currentAngleZ = 0;
    private final double smoothingFactor = 0.2;
    private final double sensitivityThreshold = 0.1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Finch myFinch = new Finch();
        Group root = new Group();
        Scene scene = new Scene(root, 800, 600, true);
        scene.setFill(Color.LIGHTGRAY);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800); // Ближе к модели
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(45); // Увеличили угол обзора
        scene.setCamera(camera);

        // Загрузка модели girl
        Group girlModel = loadGirlModel();
        girlModel.getTransforms().addAll(rotateX, rotateY, rotateZ);

        // Настройка освещения
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-300);
        keyLight.setTranslateY(-200);
        keyLight.setTranslateZ(-500);

        PointLight fillLight = new PointLight(Color.WHITESMOKE);
        fillLight.setTranslateX(300);
        fillLight.setTranslateY(100);
        fillLight.setTranslateZ(-400);

        root.getChildren().addAll(girlModel, keyLight, fillLight);

        primaryStage.setTitle("3D Girl с Finch Robot");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Чувствительность (можно настроить)
        double sensitivityX = 8.0;
        double sensitivityY = 8.0;
        double sensitivityZ = 8.0;

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                accelerationArray = myFinch.getAcceleration();

                double targetAngleX = accelerationArray[0] * sensitivityX;
                double targetAngleY = accelerationArray[1] * sensitivityY;
                double targetAngleZ = accelerationArray[2] * sensitivityZ;

                currentAngleX = applySmoothing(currentAngleX, targetAngleX);
                currentAngleY = applySmoothing(currentAngleY, targetAngleY);
                currentAngleZ = applySmoothing(currentAngleZ, targetAngleZ);

                rotateX.setAngle(currentAngleX);
                rotateY.setAngle(currentAngleY);
                rotateZ.setAngle(currentAngleZ);
            }
        };
        timer.start();

        primaryStage.setOnCloseRequest(event -> {
            myFinch.disconnect();
            timer.stop();
        });
    }

    private Group loadGirlModel() {
        try {
            // 1. Загрузка модели
            ObjModelImporter importer = new ObjModelImporter();
            String modelPath = getClass().getResource("/girl/girl.obj").toExternalForm();
            importer.read(modelPath);

            // 2. Создание группы модели
            Group modelGroup = new Group(importer.getImport());

            // 3. Настройка масштаба и положения
            modelGroup.setScaleX(0.8);
            modelGroup.setScaleY(0.8);
            modelGroup.setScaleZ(0.8);
            modelGroup.setTranslateY(100);

            // 4. Загрузка текстур (пример для основных частей)
            loadTexturesForModel(modelGroup);

            return modelGroup;
        } catch (Exception e) {
            System.err.println("Ошибка загрузки модели: " + e.getMessage());
            return createFallbackModel();
        }
    }

    private void loadTexturesForModel(Group modelGroup) {
        try {
            // Основные текстуры (настройте под вашу модель)
            Image botColor = new Image(getClass().getResourceAsStream("/girl/Textures/botColor.jpg"));
            Image face = new Image(getClass().getResourceAsStream("/girl/Textures/face.png"));
            Image body = new Image(getClass().getResourceAsStream("/girl/Textures/body.png"));
            Image COLORS = new Image(getClass().getResourceAsStream("/girl/Textures/COLORS.jpg"));
            Image topColor = new Image(getClass().getResourceAsStream("/girl/Textures/topColor.png"));
            Image topNormal = new Image(getClass().getResourceAsStream("/girl/Textures/topNormal.png"));

            // Применяем текстуры к соответствующим частям модели
            for (Node node : modelGroup.getChildren()) {
                if (node instanceof MeshView) {
                    MeshView mesh = (MeshView) node;
                    PhongMaterial material = new PhongMaterial();

                    // Определяем часть модели по имени (настройте под вашу модель)
                    if (mesh.getId() != null) {
                        material.setDiffuseMap(botColor);
                        material.setDiffuseMap(face);
                        material.setDiffuseMap(body);
                        material.setDiffuseMap(COLORS);
                        material.setDiffuseMap(topColor);
                        material.setDiffuseMap(topNormal);
                    }

                    // Настройки материала
                    material.setSpecularColor(Color.WHITE);
                    material.setSpecularPower(20);
                    mesh.setMaterial(material);
                }
            } // commit
        } catch (Exception e) {
            System.err.println("Ошибка загрузки текстур: " + e.getMessage());
        }
    }

    private Group createFallbackModel() {
        Box box = new Box(100, 100, 100);
        box.setMaterial(new PhongMaterial(Color.PINK));
        return new Group(box);
    }

    private double applySmoothing(double current, double target) {
        if (Math.abs(target - current) < sensitivityThreshold) {
            return current;
        }
        return lerp(current, target, smoothingFactor);
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
}