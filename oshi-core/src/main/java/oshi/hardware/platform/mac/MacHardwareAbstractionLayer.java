/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import oshi.hardware.*;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;

public class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public ComputerSystem getComputerSystem() {
        if (this.computerSystem == null) {
            this.computerSystem = new MacComputerSystem();
        }
        return this.computerSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new MacCentralProcessor();
        }
        return this.processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new MacGlobalMemory();
        }
        return this.memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PowerSource[] getPowerSources() {
        return MacPowerSource.getPowerSources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HWDiskStore[] getDiskStores() {
        return new MacDisks().getDisks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Display[] getDisplays() {
        return MacDisplay.getDisplays();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new MacSensors();
        }
        return this.sensors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworkIFs() {
        return new MacNetworks().getNetworks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsbDevice[] getUsbDevices(boolean tree) {
        return MacUsbDevice.getUsbDevices(tree);
    }

    /**
     * Instantiates an array of {@link SoundCard} objects, representing the
     * Sound cards.
     *
     * @return An array of SoundCard objects or an empty array if none are
     *         present.
     */
    @Override
    public SoundCard[] getSoundCards() {
        return new SoundCard[0];
    }
}
