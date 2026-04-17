/**
 * Histórico de comandos só da sessão atual do prompt (modal).
 * Máximo 50 entradas; navegação ↑/↓ estilo shell (FIFO ao encher).
 */
export class SessionCommandHistory {
  private readonly maxEntries = 50;
  private readonly items: string[] = [];
  /** -1 = editando linha nova; >=0 = índice em {@link items} */
  private browseIndex = -1;
  private draft = '';

  push(cmd: string): void {
    const t = cmd.trim();
    if (t === '') {
      return;
    }
    const last = this.items.length > 0 ? this.items[this.items.length - 1] : '';
    if (t === last) {
      this.resetBrowse();
      return;
    }
    if (this.items.length >= this.maxEntries) {
      this.items.shift();
    }
    this.items.push(t);
    this.resetBrowse();
  }

  resetBrowse(): void {
    this.browseIndex = -1;
    this.draft = '';
  }

  /** Seta ao submeter Enter com linha atual (pode ser vazia para guardar rascunho). */
  up(currentLine: string): string {
    if (this.items.length === 0) {
      return currentLine;
    }
    if (this.browseIndex === -1) {
      this.draft = currentLine;
      this.browseIndex = this.items.length - 1;
    } else if (this.browseIndex > 0) {
      this.browseIndex--;
    }
    return this.items[this.browseIndex];
  }

  /** Com ↑ em uso; se não estiver navegando, devolve a linha atual. */
  down(currentLine: string): string {
    if (this.browseIndex === -1) {
      return currentLine;
    }
    if (this.browseIndex < this.items.length - 1) {
      this.browseIndex++;
      return this.items[this.browseIndex];
    }
    this.browseIndex = -1;
    return this.draft;
  }
}
