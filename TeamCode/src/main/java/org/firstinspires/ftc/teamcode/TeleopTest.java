package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.CRServo;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCompatibilityManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.usb.UsbConstants;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagClusterDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection;
import java.util.List;
import java.lang.Math;

@TeleOp(name="Basic: Linear OpMode", group="Linear OpMode")
public class TeleopTest extends LinearOpMode {

    // Declare OpMode members.
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor frontRightDrive = null;
    private DcMotor frontLeftDrive = null;
    private DcMotor backRightDrive = null;
    private DcMotor backLeftDrive = null;
    private DcMotor intakeMotor = null;
    private CRServo leftIntakeServo = null;

    private DcMotor flyWheel = null;


    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).
        frontLeftDrive  = hardwareMap.get(DcMotor.class, "front_left_drive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "front_right_drive");
        backLeftDrive  = hardwareMap.get(DcMotor.class, "back_left_drive");
        backRightDrive = hardwareMap.get(DcMotor.class, "back_right_drive");
        intakeMotor = hardwareMap.get(DcMotor.class, "intake");
        leftIntakeServo = hardwareMap.get(CRServo.class, "leftintake");
        flyWheel = hardwareMap.get(DcMotor.class, "shooter");


        // To drive forward, most robots need the motor on one side to be reversed, because the axles point in opposite directions.
        // Pushing the left stick forward MUST make robot go forward. So adjust these two lines based on your first test drive.
        // Note: The settings here assume direct drive on left and right wheels.  Gear Reduction or 90 Deg drives may require direction flips
        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);
        intakeMotor.setDirection(DcMotor.Direction.FORWARD);
        leftIntakeServo.setDirection(CRServo.Direction.FORWARD);
        flyWheel.setDirection(DcMotor.Direction.FORWARD);

        // zero out encoder and then set it up or smthn
        frontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        aprilTag = new AprilTagProcessor.Builder().build();
        VisionPortal.Builder builder = new VisionPortal.Builder();
        builder.setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"));
        builder.enableLiveView(true);
        builder.addProcessor(aprilTag);
        visionPortal = builder.build();

        // Wait for the game to start (driver presses START)
        waitForStart();
        runtime.reset();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            visionPortal.resumeStreaming();

            // take a peek at el april tag and figure out how far it is and stuff
            List<AprilTagDetection> currDetections = aprilTag.getDetections();
            Vector3 origin = new Vector3(0, 0, 0);
            double minDist = Double.POSITIVE_INFINITY;
            Vector3 closestPos = null;
            for (AprilTagDetection detection : currDetections) {
                if (!(detection instanceof AprilTagSingleDetection)) {
                    AprilTagClusterDetection clusterDet = (AprilTagClusterDetection) detection;

                    // whren the langaugs doenz't havgfe duck typkjw
                    Vector3 tagPos = new Vector3(detection.ftcPose.x, detection.ftcPose.y, detection.ftcPose.z);
                    double dist = tagPos.calculateDistance(origin);
                    // it's close to the robot and it isn't upside down, so it can be scored in
                    if (dist < minDist && Math.abs(detection.ftcPose.roll) <= 90 && clusterDet.metadata.name.split(" ")[0].equals("RED")) {
                        minDist = dist;
                        closestPos = tagPos;
                        // remember to note which tag dist came from
                    }
                }
            }

            if (closestPos == null) {
                telemetry.addLine("no april tags detected");
            } else {
                telemetry.addData("closest april tag is ", closestPos);

                if (closestPos.z < 15 && gamepad1.a) {
                    frontLeftDrive.setPower(0.2);
                    frontRightDrive.setPower(0.2);
                    backLeftDrive.setPower(0.2);
                    backRightDrive.setPower(0.2);
                }
            }

            // try running servo (doesn't work even though i literally spent three hours working on it)
            leftIntakeServo.setPower(1);

            // Choose to drive using either Tank Mode, or POV Mode
            // Comment out the method that's not used.  The default below is POV.

            double drive = -gamepad1.left_stick_y;
            double turn = gamepad1.right_stick_x;
            double strafe = gamepad1.left_stick_x;

            if (gamepad2.x) {
                intakeMotor.setPower(1);
            } else if (gamepad2.a) {
                intakeMotor.setPower(-1);
            } else {
                intakeMotor.setPower(0);
            }
            if (gamepad2.y) {
                flyWheel.setPower(1);
            }
            else {
                flyWheel.setPower(0);
            }

            double fLeftPow = Range.clip(drive + turn + strafe, -1, 1);
            double bLeftPow = Range.clip(drive + turn - strafe, -1, 1);
            double fRightPow = Range.clip(drive - turn - strafe, -1, 1);
            double bRightPow = Range.clip(drive - turn + strafe, -1, 1);

            frontLeftDrive.setPower(fLeftPow);
            backLeftDrive.setPower(bLeftPow);
            frontRightDrive.setPower(fRightPow);
            backRightDrive.setPower(bRightPow);

            // Show the elapsed game time and wheel power.
            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.addData("motor pos", frontLeftDrive.getCurrentPosition());
            telemetry.update();
        }
    }
}
class Vector3 {
    public double x;
    public double y;
    public double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public double calculateDistance(Vector3 other) {
        return Math.sqrt(
                (this.x - other.x) * (this.x - other.x) +
                (this.y - other.y) * (this.y - other.y) +
                (this.z - other.z) * (this.z - other.z)
        );
    }
    @Override
    public String toString() {
        return this.x + ", " + this.y + ", " + this.z;
    }
}