package com.byd.voiceassistant.ai.router;

import com.byd.voiceassistant.vehicle.VehicleAction;
import org.junit.Assert;
import org.junit.Test;

public class SemanticRouterTest {

    @Test
    public void testExactCarCommandRoutesAboveThreshold() {
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        SemanticRouterResult result = router.route("turn on the air conditioner");
        Assert.assertTrue(result.isMatch());
        Assert.assertTrue(result.getConfidence() >= 0.70f);
        Assert.assertTrue(result.getAction() instanceof VehicleAction.SetAcEnabled);
        Assert.assertTrue(((VehicleAction.SetAcEnabled) result.getAction()).isEnabled());
    }

    @Test
    public void testTemperatureAdjustmentParsing() {
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        SemanticRouterResult result = router.route("set temperature to 23 degrees");
        Assert.assertTrue(result.isMatch());
        Assert.assertTrue(result.getConfidence() >= 0.70f);
        Assert.assertTrue(result.getAction() instanceof VehicleAction.SetAcTemperature);
        Assert.assertEquals(23.0f, ((VehicleAction.SetAcTemperature) result.getAction()).getTargetCelsius(), 0.01f);
    }

    @Test
    public void testGeneralQueryFallsBelowThreshold() {
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        SemanticRouterResult result = router.route("Who was the first president of Brazil?");
        Assert.assertFalse(result.isMatch());
        Assert.assertTrue(result.getConfidence() < 0.70f);
        Assert.assertNull(result.getAction());
    }

    @Test
    public void testInformationalTemperatureQueryDoesNotRoute() {
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        SemanticRouterResult result = router.route("What was the temperature in Shenzhen yesterday? It was 25 degrees.");
        Assert.assertFalse(result.isMatch());
    }

    @Test
    public void testSeatHeatingAndFanSpeedRouting() {
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        SemanticRouterResult seatResult = router.route("turn on driver seat heating");
        Assert.assertTrue(seatResult.isMatch());
        Assert.assertTrue(seatResult.getAction() instanceof VehicleAction.SetSeatHeating);

        SemanticRouterResult fanResult = router.route("set fan speed to 4");
        Assert.assertTrue(fanResult.isMatch());
        Assert.assertTrue(fanResult.getAction() instanceof VehicleAction.SetFanSpeed);
        Assert.assertEquals(4, ((VehicleAction.SetFanSpeed) fanResult.getAction()).getSpeed());
    }
}
