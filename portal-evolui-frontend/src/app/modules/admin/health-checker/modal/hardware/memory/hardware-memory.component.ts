import {Component, Input} from '@angular/core';
import {Memory} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'hardware-memory',
    templateUrl: './hardware-memory.component.html',
    styleUrls: ['./hardware-memory.component.scss'],

    standalone: false
})
export class HardwareMemoryComponent {
    @Input() data: Memory | null = null;

    isValidValue(value: any): boolean {
        return value !== null && value !== undefined && value !== '' && value !== 'unknown';
    }

    getDisplayValue(value: any): string {
        return this.isValidValue(value) ? value : 'N/A';
    }

    formatBytes(bytes: number): string {
        if (!this.isValidValue(bytes) || bytes === 0) {
            return 'N/A';
        }

        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${sizes[i]}`;
    }

    formatClockSpeed(speed: number): string {
        if (!this.isValidValue(speed) || speed === 0) {
            return 'N/A';
        }

        const mhz = speed / 1000000;
        return `${mhz.toFixed(0)} MHz`;
    }

    getMemoryUsagePercentage(): number {
        if (!this.hasValidData() || !this.isValidValue(this.data!.total) || !this.isValidValue(this.data!.available)) {
            return 0;
        }

        const used = this.data!.total - this.data!.available;
        return (used / this.data!.total) * 100;
    }

    getSwapUsagePercentage(): number {
        if (!this.hasVirtualMemory() || !this.isValidValue(this.data!.virtualMemory.swapTotal) || !this.isValidValue(this.data!.virtualMemory.swapUsed)) {
            return 0;
        }

        return (this.data!.virtualMemory.swapUsed / this.data!.virtualMemory.swapTotal) * 100;
    }

    getVirtualUsagePercentage(): number {
        if (!this.hasVirtualMemory() || !this.isValidValue(this.data!.virtualMemory.virtualMax) || !this.isValidValue(this.data!.virtualMemory.virtualInUse)) {
            return 0;
        }

        return (this.data!.virtualMemory.virtualInUse / this.data!.virtualMemory.virtualMax) * 100;
    }

    getUsedMemory(): number {
        if (!this.hasValidData() || !this.isValidValue(this.data!.total) || !this.isValidValue(this.data!.available)) {
            return 0;
        }

        return this.data!.total - this.data!.available;
    }

    getMemoryStatusClass(percentage: number): string {
        if (!this.isValidValue(percentage)) {
            return 'bg-gray-100 text-gray-800';
        }

        if (percentage < 50) {
            return 'bg-green-100 text-green-800';
        } else if (percentage < 80) {
            return 'bg-yellow-100 text-yellow-800';
        } else {
            return 'bg-red-100 text-red-800';
        }
    }

    getMemoryBarClass(percentage: number): string {
        if (!this.isValidValue(percentage)) {
            return 'bg-gray-300';
        }

        if (percentage < 50) {
            return 'bg-green-500';
        } else if (percentage < 80) {
            return 'bg-yellow-500';
        } else {
            return 'bg-red-500';
        }
    }

    getMemoryStatus(percentage: number): string {
        if (!this.isValidValue(percentage)) {
            return 'Desconhecido';
        }

        if (percentage < 50) {
            return 'Normal';
        } else if (percentage < 80) {
            return 'Moderado';
        } else {
            return 'Alto';
        }
    }

    hasValidData(): boolean {
        return this.data !== null && this.data !== undefined;
    }

    hasVirtualMemory(): boolean {
        return this.hasValidData() &&
            this.data!.virtualMemory !== null &&
            this.data!.virtualMemory !== undefined;
    }

    hasPhysicalMemory(): boolean {
        return this.hasValidData() &&
            this.data!.physicalMemory !== null &&
            this.data!.physicalMemory !== undefined &&
            Array.isArray(this.data!.physicalMemory) &&
            this.data!.physicalMemory.length > 0;
    }

    getTotalPhysicalCapacity(): number {
        if (!this.hasPhysicalMemory()) {
            return 0;
        }

        return this.data!.physicalMemory.reduce((total, memory) => {
            return total + (memory.capacity || 0);
        }, 0);
    }

    getMemoryModulesCount(): number {
        if (!this.hasPhysicalMemory()) {
            return 0;
        }

        return this.data!.physicalMemory.length;
    }

    getMemoryType(): string {
        if (!this.hasPhysicalMemory()) {
            return 'N/A';
        }

        const firstModule = this.data!.physicalMemory[0];
        return this.getDisplayValue(firstModule.memoryType);
    }

    getAverageClockSpeed(): number {
        if (!this.hasPhysicalMemory()) {
            return 0;
        }

        const validSpeeds = this.data!.physicalMemory
            .map(memory => memory.clockSpeed)
            .filter(speed => this.isValidValue(speed) && speed > 0);

        if (validSpeeds.length === 0) {
            return 0;
        }
        return validSpeeds.reduce((sum, speed) => sum + speed, 0) / validSpeeds.length;
    }
}
