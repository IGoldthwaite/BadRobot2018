package org.usfirst.frc.team1014.robot;

import java.util.Optional;

import org.usfirst.frc.team1014.robot.commands.Autonomous;
import org.usfirst.frc.team1014.robot.commands.Teleop;
import org.usfirst.frc.team1014.robot.subsystems.Drivetrain;
import org.usfirst.frc.team1014.robot.subsystems.Grabber;
import org.usfirst.frc.team1014.robot.subsystems.Lifter;
import org.usfirst.frc.team1014.robot.util.LogUtil;

import badlog.lib.BadLog;
import badlog.lib.DataInferMode;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
	public static OI oi;

	Drivetrain driveTrain;

	Lifter lifter;
	Grabber grabber;

	Teleop teleopCG;
	Autonomous autoCG;
	
	private BadLog logger;
	private long startTimeNS;
	private long lastLog;

	@Override
	public void robotInit() {

		startTimeNS = System.nanoTime();
		lastLog = System.currentTimeMillis();
		String session = LogUtil.genSessionName();
		System.out.println("Info: Starting session " + session);
		logger = BadLog.init("/home/lvuser/log/" + session + ".bag");
		{
			BadLog.createValue("Start Time", LogUtil.getTimestamp());
			BadLog.createValue("Event Name", Optional.ofNullable(DriverStation.getInstance().getEventName()).orElse(""));
			BadLog.createValue("Match Type", DriverStation.getInstance().getMatchType().toString());
			BadLog.createValue("Match Number", "" + DriverStation.getInstance().getMatchNumber());
			BadLog.createValue("Alliance", DriverStation.getInstance().getAlliance().toString());
			BadLog.createValue("Location", "" + DriverStation.getInstance().getLocation());

			BadLog.createTopicSubscriber("Time", "s", DataInferMode.DEFAULT, "hide", "delta", "xaxis");

			BadLog.createTopicStr("System/Browned Out", "bool", () -> LogUtil.fromBool(RobotController.isBrownedOut()));
			BadLog.createTopic("System/Battery Voltage", "V", () -> RobotController.getBatteryVoltage());
			BadLog.createTopicStr("System/FPGA Active", "bool", () -> LogUtil.fromBool(RobotController.isSysActive()));
			BadLog.createTopic("Match Time", "s", () -> DriverStation.getInstance().getMatchTime());

			oi = new OI();
			driveTrain = new Drivetrain();
			grabber = new Grabber();
			lifter = new Lifter();

			teleopCG = new Teleop(driveTrain, grabber, lifter);
			autoCG = new Autonomous(driveTrain, lifter, grabber);
		
		}
		logger.finishInitialization();
	}

	@Override
	public void autonomousInit() {
		Scheduler.getInstance().removeAll();
		
		autoCG.start();
	}

	@Override
	public void teleopInit() {
		Scheduler.getInstance().removeAll();

		teleopCG.start();
	}

	@Override
	public void testInit() {
		Scheduler.getInstance().removeAll();
	}

	@Override
	public void disabledInit() {
		Scheduler.getInstance().removeAll();
	}

	@Override
	public void autonomousPeriodic() {
		periodic();
	}

	@Override
	public void teleopPeriodic() {
		periodic();
	}

	@Override
	public void testPeriodic() {
		periodic();
	}

	@Override
	public void disabledPeriodic() {
		periodic();
	}

	private void periodic() {
		double currentTime = ((double) (System.nanoTime() - startTimeNS)) / 1_000_000_000d;
		BadLog.publish("Time", currentTime);

		Scheduler.getInstance().run();

		logger.updateTopics();
		// Only log once every 250ms in disabled, to save disk space
		long currentMS = System.currentTimeMillis();
		if (!DriverStation.getInstance().isDisabled() || (currentMS - lastLog) >= 250) {
			lastLog = currentMS;
			logger.log();
		}
	}
}
