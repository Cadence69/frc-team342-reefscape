// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;


import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.Timer;

import com.revrobotics.spark.*;

import static frc.robot.Constants.WristConstants.*;

import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkLowLevel.MotorType;



public class Wrist extends SubsystemBase {
  private final SparkMax wrist;

  private final SparkMaxConfig wristConfig;

  private final RelativeEncoder wristEncoder;
  private final DutyCycleEncoder throughBore;

  private final SparkClosedLoopController wristController;

  private final Timer timer;

  private boolean goingDown;
  private boolean didReset;

  private double speed;
  private double currentPosition;
  private double desiredPosition;
  private double time;

  /** Creates a new Wrist. */
  public Wrist() {
    // Wrist instantiatiion
    wrist = new SparkMax(WRIST_ID, MotorType.kBrushless);
    wristConfig = new SparkMaxConfig();
    goingDown = false;
    didReset = false;

    timer = new Timer();

    //Encoder instantiation
    throughBore = new DutyCycleEncoder(THROUGHBORE_PORT, (2 * Math.PI), WRIST_ZERO);
    currentPosition = throughBore.get();
      
    //Wrist idle mode & smart current limit
    wristConfig
      .idleMode(IdleMode.kBrake)
      .smartCurrentLimit(WRIST_CURRENT_LIMIT);

    wristEncoder = wrist.getEncoder();
    wristEncoder.setPosition(throughBore.get());

    //Conversion factor so PID controller is able to read exactly where the throughBore is at
    wristConfig.encoder
      .positionConversionFactor(WRIST_POSITION_CONVERSION);

    wristController = wrist.getClosedLoopController();

    wristConfig.closedLoop.p(WRIST_PID_VALUES[0]);
    wristConfig.closedLoop.i(WRIST_PID_VALUES[1]);
    wristConfig.closedLoop.d(WRIST_PID_VALUES[2]);
    wristConfig.closedLoop.outputRange(-0.3, .3);

    wrist.configure(wristConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    timer.start();
  }

  /*
   * Moves wrist at a specified speed
   */
  public void move(double speed){
    wrist.set(speed);
  }

  /*
   * Moves wrist to a certain position
   */
  public void wristToPosition(double position){
    this.desiredPosition = position;
    wristController.setReference(position, ControlType.kPosition);
  }

  /*
   * Returns the throughbore encoder
   */
  public DutyCycleEncoder getThroughBore(){
    return throughBore;
  }

  /**resets relative encoder to equal the through bore value*/
  public void resetEncoder(){
    wristEncoder.setPosition(throughBore.get());
    didReset = true;
  }

  /**
   * VERY IMPORTANT!!! Makes sure the wrist is in the safe range so the robot doesn't critically damage itself
   * @return
   */
  public boolean isSafe(){
    return (currentPosition >= 1.3);
  }

  @Override
  public void periodic() {
    time = timer.get();
    currentPosition = wristEncoder.getPosition();
    if(time >= 1.5 && !didReset)
      resetEncoder();
  }

  @Override
  public void initSendable(SendableBuilder builder){
    super.initSendable(builder);
    builder.setSmartDashboardType("Wrist");
    builder.addBooleanProperty("Is Safe", () -> isSafe(), null);
      // if(true){
        builder.addDoubleProperty("Error", () -> throughBore.get() - wristEncoder.getPosition(), null);
        builder.addDoubleProperty("ThroughBore", () -> throughBore.get(), null);
        builder.addDoubleProperty("Encoder", () -> wristEncoder.getPosition(), null);
        builder.addDoubleProperty("Speed", () -> wristEncoder.getVelocity(), null);
        builder.addDoubleProperty("Desired Position", () -> desiredPosition, null);
      // }
  }
}
