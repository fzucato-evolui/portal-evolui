import {Component, Input} from '@angular/core';
import {Processor} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'hardware-processor',
    templateUrl: './hardware-processor.component.html',
    styleUrls: ['./hardware-processor.component.scss'],

    standalone: false
})
export class HardwareProcessorComponent {
    @Input() data: Processor | null = null;

    isValidValue(value: any): boolean {
        return value !== null && value !== undefined && value !== '' && value !== 'unknown';
    }

    getDisplayValue(value: any): string {
        return this.isValidValue(value) ? value : 'N/A';
    }

    formatFrequency(frequency: number): string {
        if (!this.isValidValue(frequency) || frequency === 0) {
            return 'N/A';
        }

        const ghz = frequency / 1000000000;
        return `${ghz.toFixed(2)} GHz`;
    }

    formatCpuLoad(load: number): string {
        if (!this.isValidValue(load)) {
            return 'N/A';
        }

        return `${load.toFixed(1)}%`;
    }

    getCpuLoadBarClass(load: number): string {
        if (!this.isValidValue(load)) {
            return 'bg-gray-300';
        }

        if (load < 30) {
            return 'bg-green-500';
        } else if (load < 70) {
            return 'bg-yellow-500';
        } else {
            return 'bg-red-500';
        }
    }

    getCpuLoadStatusClass(load: number): string {
        if (!this.isValidValue(load)) {
            return 'bg-gray-100 text-gray-800';
        }

        if (load < 30) {
            return 'bg-green-100 text-green-800';
        } else if (load < 70) {
            return 'bg-yellow-100 text-yellow-800';
        } else {
            return 'bg-red-100 text-red-800';
        }
    }

    getCpuLoadStatus(load: number): string {
        if (!this.isValidValue(load)) {
            return 'Desconhecido';
        }

        if (load < 30) {
            return 'Baixo';
        } else if (load < 70) {
            return 'Moderado';
        } else {
            return 'Alto';
        }
    }

    hasValidData(): boolean {
        return this.data !== null && this.data !== undefined;
    }

    hasProcessorIdentifier(): boolean {
        return this.hasValidData() &&
            this.data!.processorIdentifier !== null &&
            this.data!.processorIdentifier !== undefined;
    }

    getArchitecture(): string {
        if (!this.hasProcessorIdentifier()) {
            return 'N/A';
        }

        const is64bit = this.data!.processorIdentifier.cpu64bit;
        return is64bit ? '64-bit' : '32-bit';
    }

    getThreadsPerCore(): number {
        if (!this.hasValidData() ||
            !this.isValidValue(this.data!.logicalProcessorCount) ||
            !this.isValidValue(this.data!.physicalProcessorCount)) {
            return 0;
        }

        return this.data!.logicalProcessorCount / this.data!.physicalProcessorCount;
    }
}
