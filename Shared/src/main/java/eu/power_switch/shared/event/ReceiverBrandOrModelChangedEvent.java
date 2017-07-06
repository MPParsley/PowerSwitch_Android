package eu.power_switch.shared.event;

import eu.power_switch.shared.Brand;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Created by Markus on 02.07.2017.
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Value
public class ReceiverBrandOrModelChangedEvent extends EventBusEvent {

    String model;

    Brand brand;

}
