package com.byd.voiceassistant.vehicle;

import org.junit.Assert;
import org.junit.Test;

public class VehicleControllerTest {

    @Test
    public void testSetTemperatureWithinBounds() {
        SimulatedVehicleController controller = new SimulatedVehicleController();
        CarExecutionResult result = controller.execute(new VehicleAction.SetAcTemperature(24.5f));
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(24.5f, controller.getCurrentState().getCabinTemperature(), 0.01f);
        Assert.assertTrue(result.getSpeechResponse().contains("24.5"));
    }

    @Test
    public void testSetTemperatureClamping() {
        SimulatedVehicleController controller = new SimulatedVehicleController();
        // Out of bounds: 40°C should clamp to 32.0°C max
        CarExecutionResult resultMax = controller.execute(new VehicleAction.SetAcTemperature(40.0f));
        Assert.assertTrue(resultMax.isSuccess());
        Assert.assertEquals(32.0f, controller.getCurrentState().getCabinTemperature(), 0.01f);

        // Out of bounds: 10°C should clamp to 16.0°C min
        CarExecutionResult resultMin = controller.execute(new VehicleAction.SetAcTemperature(10.0f));
        Assert.assertTrue(resultMin.isSuccess());
        Assert.assertEquals(16.0f, controller.getCurrentState().getCabinTemperature(), 0.01f);
    }

    @Test
    public void testToggleAc() {
        SimulatedVehicleController controller = new SimulatedVehicleController();
        controller.execute(new VehicleAction.SetAcEnabled(true));
        Assert.assertTrue(controller.getCurrentState().isAcEnabled());
        controller.execute(new VehicleAction.SetAcEnabled(false));
        Assert.assertFalse(controller.getCurrentState().isAcEnabled());
    }

    @Test
    public void testWindowControl() {
        SimulatedVehicleController controller = new SimulatedVehicleController();
        CarExecutionResult result = controller.execute(new VehicleAction.SetWindowPosition(WindowPosition.ALL, 100));
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(Integer.valueOf(100), controller.getCurrentState().getWindows().get(WindowPosition.FRONT_LEFT));
    }
}
