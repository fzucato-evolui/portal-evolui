import {Component, Input} from '@angular/core';
import {ComputerSystem} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'hardware-info',
    templateUrl: './hardware-info.component.html',
    styleUrls: ['./hardware-info.component.scss'],

    standalone: false
})
export class HardwareInfoComponent {
    @Input() data: ComputerSystem | null = null;

    isValidValue(value: any): boolean {
        return value !== null && value !== undefined && value !== '' && value !== 'unknown';
    }

    getDisplayValue(value: any): string {
        return this.isValidValue(value) ? value : 'N/A';
    }

    formatReleaseDate(dateString: string): string {
        if (!this.isValidValue(dateString)) {
            return 'N/A';
        }

        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('pt-BR');
        } catch {
            return dateString;
        }
    }

    hasValidData(): boolean {
        return this.data !== null && this.data !== undefined;
    }

    hasFirmwareInfo(): boolean {
        return this.hasValidData() &&
            this.data!.firmware !== null &&
            this.data!.firmware !== undefined;
    }

    hasBaseboardInfo(): boolean {
        return this.hasValidData() &&
            this.data!.baseboard !== null &&
            this.data!.baseboard !== undefined;
    }
}
