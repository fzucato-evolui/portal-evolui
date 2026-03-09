import {Component, Input} from '@angular/core';
import {DiskStore} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'hardware-diskstore',
    templateUrl: './hardware-diskstore.component.html',
    styleUrls: ['./hardware-diskstore.component.scss'],

    standalone: false
})
export class HardwareDiskstoreComponent {
    @Input() data: DiskStore[] | null = null;

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

    formatNumber(num: number): string {
        if (!this.isValidValue(num)) {
            return 'N/A';
        }

        return num.toLocaleString('pt-BR');
    }

    formatTime(milliseconds: number): string {
        if (!this.isValidValue(milliseconds) || milliseconds === 0) {
            return 'N/A';
        }

        const seconds = milliseconds / 1000;
        if (seconds < 60) {
            return `${seconds.toFixed(1)}s`;
        } else if (seconds < 3600) {
            return `${(seconds / 60).toFixed(1)}m`;
        } else {
            return `${(seconds / 3600).toFixed(1)}h`;
        }
    }

    getDiskTypeIcon(model: string): string {
        if (!this.isValidValue(model)) {
            return 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4';
        }

        const modelLower = model.toLowerCase();
        if (modelLower.includes('nvme') || modelLower.includes('ssd')) {
            return 'M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z';
        } else {
            return 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4';
        }
    }

    getDiskTypeClass(model: string): string {
        if (!this.isValidValue(model)) {
            return 'model-gray-600';
        }

        const modelLower = model.toLowerCase();
        if (modelLower.includes('nvme') || modelLower.includes('ssd')) {
            return 'model-green-600';
        } else {
            return 'model-blue-600';
        }
    }

    getDiskType(model: string): string {
        if (!this.isValidValue(model)) {
            return 'Desconhecido';
        }

        const modelLower = model.toLowerCase();
        if (modelLower.includes('nvme')) {
            return 'NVMe SSD';
        } else if (modelLower.includes('ssd')) {
            return 'SSD';
        } else {
            return 'HDD';
        }
    }

    getActivityLevel(reads: number, writes: number): string {
        if (!this.isValidValue(reads) || !this.isValidValue(writes)) {
            return 'Desconhecido';
        }

        const totalOps = reads + writes;
        if (totalOps < 10000) {
            return 'Baixo';
        } else if (totalOps < 100000) {
            return 'Moderado';
        } else {
            return 'Alto';
        }
    }

    getActivityClass(reads: number, writes: number): string {
        const activity = this.getActivityLevel(reads, writes);
        switch (activity) {
            case 'Baixo':
                return 'model-badge-green';
            case 'Moderado':
                return 'model-badge-yellow';
            case 'Alto':
                return 'model-badge-red';
            default:
                return 'model-badge-gray';
        }
    }

    getTotalCapacity(): number {
        if (!this.hasValidData()) {
            return 0;
        }

        return this.data!.reduce((total, disk) => {
            return total + (disk.size || 0);
        }, 0);
    }

    getTotalReadBytes(): number {
        if (!this.hasValidData()) {
            return 0;
        }

        return this.data!.reduce((total, disk) => {
            return total + (disk.readBytes || 0);
        }, 0);
    }

    getTotalWriteBytes(): number {
        if (!this.hasValidData()) {
            return 0;
        }

        return this.data!.reduce((total, disk) => {
            return total + (disk.writeBytes || 0);
        }, 0);
    }

    hasValidData(): boolean {
        return this.data !== null && this.data !== undefined && Array.isArray(this.data) && this.data.length > 0;
    }

    hasPartitions(disk: DiskStore): boolean {
        return disk.partitions !== null &&
            disk.partitions !== undefined &&
            Array.isArray(disk.partitions) &&
            disk.partitions.length > 0;
    }

    getDiskIndex(index: number): string {
        return `Disco ${index + 1}`;
    }

    getShortModel(model: string): string {
        if (!this.isValidValue(model)) {
            return 'N/A';
        }

        if (model.length > 50) {
            return model.substring(0, 47) + '...';
        }
        return model;
    }

    getReadWriteRatio(reads: number, writes: number): string {
        if (!this.isValidValue(reads) || !this.isValidValue(writes) || writes === 0) {
            return 'N/A';
        }

        const ratio = reads / writes;
        return `${ratio.toFixed(1)}:1`;
    }
}
