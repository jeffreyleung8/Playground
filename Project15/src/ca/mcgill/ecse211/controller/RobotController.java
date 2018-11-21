
package ca.mcgill.ecse211.controller;

import ca.mcgill.ecse211.main.Main;
import ca.mcgill.ecse211.odometer.Odometer;
import ca.mcgill.ecse211.odometer.OdometerExceptions;
import ca.mcgill.ecse211.odometer.OdometryCorrection;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.RegulatedMotor;


/** This class serves to control the robot's motors
 * 
 * @author Jeffrey Leung
 */

public class RobotController {

	// Motor objects
	private  static EV3LargeRegulatedMotor leftMotor;
	private  static EV3LargeRegulatedMotor rightMotor;

	//Constants
	public final int speed = 200;
	public final int FORWARD_SPEED = 250;
	public final int ROTATE_SPEED = 100;
	public final double TRACK = Main.TRACK;
	public final double WHEEL_RAD = Main.WHEEL_RAD;
	public final double TILE_SIZE = Main.TILE_SIZE;
	public final double SENSOR_LENGTH = Main.SENSOR_LENGTH;

	//Odometer
	private Odometer odometer;

	private OdometryCorrection odoCorr;
	/**
	 * This is a constructor for the RobotController class
	 * @param odometer
	 * @param leftMotor
	 * @param rightMotor
	 */
	public RobotController(Odometer odometer,EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.odometer = odometer;
		//this.odoCorr = odoCorr;
	}

	public void directTravelTo(int x, int y) {

		double currx = odometer.getXYT()[0];
		double curry = odometer.getXYT()[1];

		double deltax = (x*TILE_SIZE) - currx;
		double deltay = (y*TILE_SIZE) - curry;

		double mTheta = Math.toDegrees(Math.atan2(deltax, deltay));

		double hypot = Math.hypot(deltax, deltay);

		// Turn to the correct angle towards the endpoint
		turnTo(mTheta);

		leftMotor.setSpeed(FORWARD_SPEED);
		rightMotor.setSpeed(FORWARD_SPEED);

		leftMotor.rotate(convertDistance(WHEEL_RAD, hypot), true);
		rightMotor.rotate(convertDistance(WHEEL_RAD, hypot), false);

		// stop vehicle
		leftMotor.stop(true);
		rightMotor.stop(true);

		Sound.beep();
	}
	/**
	 * A method to drive our vehicle to a certain Cartesian coordinate
	 * 
	 * @param x x-Coordinate
	 * @param y y-Coordinate
	 */
	public void travelTo(int x, int y) {

		int deltax = 0, deltay = 0;

		//Nearest waypoint
		int lastX = (int) Math.round(odometer.getXYT()[0] / TILE_SIZE);
		int lastY = (int) Math.round(odometer.getXYT()[1] / TILE_SIZE);


		// Angle to turn to to go to the next point
		double corrTheta = 0;

		setSpeeds(ROTATE_SPEED, ROTATE_SPEED);

		//Check if x-component is different
		if(lastX - x != 0) {
			if (lastX > x) {
				corrTheta = 270;
				deltax = -1;
			} 
			else {
				corrTheta = 90;
				deltax = 1;
			}

			// Rotate to the proper angle
			if (lastX != x)
				turnTo(corrTheta);

			// Number of tiles to move in X
			int tilesX = x - lastX;

			setSpeeds(150, 150);

			// Advance towards next point's x coordinate
			for (int i = 1; i <= Math.abs(tilesX); i++) {

				// Correction at first line
				if (i == 1) {
					odoCorr.correct(corrTheta);
				}

				// travelToDirect() to the next closest point
				setSpeeds(150,150);

				directTravelTo(lastX + deltax*i, lastY);

				//Correction at the line
				odoCorr.correct(corrTheta);

				// Move back by sensor_length at the last tile
				if (i == Math.abs(tilesX)) {
					this.travelDist(SENSOR_LENGTH);
				}

			}
		}
		setSpeeds(ROTATE_SPEED, ROTATE_SPEED);

		if(lastY - y != 0) {
			if (lastY > y) {
				corrTheta = 180;
				deltay = -1;
			} 
			else {
				corrTheta = 0;
				deltay = 1;
			}

			// Rotate to the proper angle
			if (lastY != y)
				turnTo(corrTheta);

			// Number of tiles to move in Y
			int tilesY = y - lastY;

			setSpeeds(150, 150);

			// Advance towards next point's y coordinate
			for (int i = 1; i <= Math.abs(tilesY); i++) {

				// Correction at first line
				if (i == 1) {
					odoCorr.correct(corrTheta);
				}

				// travelToDirect() to the next closest point
				setSpeeds(150,150);
				directTravelTo(lastX, lastY + deltay*i);

				// Correction at the line
				odoCorr.correct(corrTheta);

				// Move back by sensor_length at the last tile
				if (i == Math.abs(tilesY)) {
					this.travelDist(SENSOR_LENGTH);
				}
			}
		}

	}

	/**
	 * A method to turn our vehicle to a certain angle
	 * 
	 * @param theta
	 */
	public void turnTo(double theta) {

		// Get current theta
		double currTheta = odometer.getXYT()[2];

		// Set angle displacement
		double dTheta = theta - currTheta;

		// Set speed to turn speed
		leftMotor.setSpeed(ROTATE_SPEED);
		rightMotor.setSpeed(ROTATE_SPEED);

		// Calculate smallest angle
		if (dTheta > 180) {
			dTheta -= 360;
		} else if (dTheta < -180) {
			dTheta += 360;
		}
		if (dTheta == 180 || dTheta == -180) {
			dTheta = 180;
		}

		leftMotor.rotate(convertAngle(WHEEL_RAD, TRACK, dTheta), true);
		rightMotor.rotate(-convertAngle(WHEEL_RAD, TRACK, dTheta), false);

	}

	/**
	 * Sets the speeds of the motors.
	 * 
	 * @param leftSpeed 
	 * @param rightSpeed 
	 */
	public void setSpeeds(int leftSpeed, int rightSpeed) {
		leftMotor.setSpeed(leftSpeed);
		rightMotor.setSpeed(rightSpeed);
	}

	/**
	 * Moves the robot forward
	 */
	public void moveForward() {
		leftMotor.synchronizeWith(new RegulatedMotor[] { rightMotor });
		leftMotor.startSynchronization();
		leftMotor.forward();
		rightMotor.forward();
		leftMotor.endSynchronization();
	}

	/**
	 * Moves the robot backward 
	 */
	public void moveBackward() {
		leftMotor.synchronizeWith(new RegulatedMotor[] { rightMotor });
		leftMotor.startSynchronization();
		leftMotor.backward();
		rightMotor.backward();
		leftMotor.endSynchronization();
	}

	/**
	 * Stops the robot
	 */
	public void stopMoving() {
		leftMotor.synchronizeWith(new RegulatedMotor[] { rightMotor });
		leftMotor.startSynchronization();
		leftMotor.stop();
		rightMotor.stop();
		leftMotor.endSynchronization();

	}
	/**
	 * Stops the specified motors of the robot.
	 * 
	 * @param stopLeft 
	 * @param stopRight 
	 */
	public void stopMoving(boolean stopLeft, boolean stopRight) {
		leftMotor.synchronizeWith(new RegulatedMotor[] { rightMotor });
		leftMotor.startSynchronization();
		if (stopLeft)
			leftMotor.stop();
		if (stopRight)
			rightMotor.stop();
		leftMotor.endSynchronization();
	}

	/**
	 * Stops the specified motors of the robot.
	 * 
	 * @param stopLeft 
	 * @param stopRight 
	 */
	public void startMoving(boolean startLeft, boolean startRight) {
		if (startLeft)
			leftMotor.forward();
		if (startRight)
			rightMotor.forward();
	}
	/**
	 * Set the acceleration of the motors.
	 * 
	 * @param acceleration the acceleration to set the left and right motor to
	 */
	public void setAcceleration(int acceleration) {
		leftMotor.setAcceleration(acceleration);
		rightMotor.setAcceleration(acceleration);
	}

	/**
	 * Turns the robot by the specified angle
	 * 
	 * @param dTheta 
	 */
	public void turnBy(double dTheta, boolean clockwise) {
		if(clockwise) {
			leftMotor.rotate(convertAngle(WHEEL_RAD, TRACK, dTheta), true);
			rightMotor.rotate(-convertAngle(WHEEL_RAD, TRACK, dTheta), false);
		}
		else {
			leftMotor.rotate(-convertAngle(WHEEL_RAD, TRACK, dTheta), true);
			rightMotor.rotate(convertAngle(WHEEL_RAD, TRACK, dTheta), false);
		}
	}

	/**
	 * Moves the robot forward by the specified distance 
	 * 
	 * @param dist 
	 */
	public void travelDist(double distance) {
		leftMotor.rotate(convertDistance(WHEEL_RAD, distance), true);
		rightMotor.rotate(convertDistance(WHEEL_RAD, distance), false);
	}

	/**
	 * Rotate the robot clockwise or counterclockwise.
	 * 
	 * @param rotateClockwise 
	 * @param speed 
	 */
	public void rotate(boolean rotateClockwise) {
		if (rotateClockwise) {
			leftMotor.forward();
			rightMotor.backward();
		} else {
			leftMotor.backward();
			rightMotor.forward();
		}
	}
	/**
	 * Specifies whether the robot is current moving.
	 * 
	 * @return 
	 */
	public boolean isMoving() {
		if (leftMotor.isMoving() || rightMotor.isMoving())
			return true;
		return false;
	}

	/**
	 * This method allows the conversion of a distance to the total rotation of each
	 * wheel need to cover that distance.
	 * 
	 * @param radius
	 * @param distance
	 * @return 
	 */
	public static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	/**
	 * This method allows the conversion of a angle to the total rotation of each
	 * wheel need to cover that distance.
	 * 
	 * @param radius
	 * @param distance
	 * @param angle
	 * @return the angle the robot needs to turn each wheel to rotate
	 */
	public static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

	/**
	 * Sets the OdometryCorrection object to be used by the robot controller.
	 * 
	 * @param odoCorrection the OdometryCorrection object to be used
	 */
	public void setOdoCorrection(OdometryCorrection odoCorrection) {
		this.odoCorr = odoCorrection;
	}

}