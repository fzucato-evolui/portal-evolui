import {Component, Input} from '@angular/core';
import {FileStore, FileSystem} from "../../../../../../shared/models/health-checker.model";

@Component({
    selector: 'software-file-system',
    templateUrl: './software-file-system.component.html',
    styleUrls: ['./software-file-system.component.scss'],

    standalone: false
})
export class SoftwareFileSystemComponent {

    @Input() data: FileSystem;
    formatBytes(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    getUsagePercentage(fileStore: FileStore): number {
        if (!fileStore.totalSpace || fileStore.totalSpace === 0) return 0;
        const usedSpace = fileStore.totalSpace - fileStore.freeSpace;
        return Math.round((usedSpace / fileStore.totalSpace) * 100);
    }

    getUsageClass(percentage: number): string {
        if (percentage >= 90) return 'bg-red-500';
        if (percentage >= 75) return 'bg-yellow-500';
        if (percentage >= 50) return 'bg-blue-500';
        return 'bg-green-500';
    }

    getUsageTextClass(percentage: number): string {
        if (percentage >= 90) return 'text-red-700';
        if (percentage >= 75) return 'text-yellow-700';
        if (percentage >= 50) return 'text-blue-700';
        return 'text-green-700';
    }

    getFileDescriptorUsagePercentage(): number {
        if (!this.data || !this.data.maxFileDescriptors || this.data.maxFileDescriptors === 0) return 0;
        return Math.round((this.data.openFileDescriptors / this.data.maxFileDescriptors) * 100);
    }

    getFileDescriptorClass(): string {
        const percentage = this.getFileDescriptorUsagePercentage();
        if (percentage >= 80) return 'bg-red-500';
        if (percentage >= 60) return 'bg-yellow-500';
        return 'bg-green-500';
    }

    getDriveLabel(fileStore: FileStore): string {
        let label = fileStore.name || fileStore.mount || 'Unidade Desconhecida';
        if (fileStore.label && fileStore.label.trim() !== '') {
            label += ` (${fileStore.label})`;
        }
        return label;
    }

    getUsedSpace(fileStore: FileStore): number {
        return fileStore.totalSpace - fileStore.freeSpace;
    }

    hasValidFileStores(): boolean {
        return this.data && this.data.fileStores && this.data.fileStores.length > 0;
    }

    getTotalStorageSpace(): number {
        if (!this.hasValidFileStores()) return 0;
        return this.data.fileStores.reduce((total, store) => total + store.totalSpace, 0);
    }

    getTotalFreeSpace(): number {
        if (!this.hasValidFileStores()) return 0;
        return this.data.fileStores.reduce((total, store) => total + store.freeSpace, 0);
    }

    getTotalUsedSpace(): number {
        return this.getTotalStorageSpace() - this.getTotalFreeSpace();
    }

    getOverallUsagePercentage(): number {
        const totalSpace = this.getTotalStorageSpace();
        if (totalSpace === 0) return 0;
        return Math.round((this.getTotalUsedSpace() / totalSpace) * 100);
    }

    trackByUuid(index: number, fileStore: FileStore): string {
        return fileStore.uuid || index.toString();
    }
}
