package br.com.evolui.healthchecker.interfaces;

import br.com.evolui.portalevolui.shared.dto.ConsoleResponseMessageDTO;

public interface IConsoleHandler {
    void onInput(ConsoleResponseMessageDTO value);
    void onError(ConsoleResponseMessageDTO value);
    void onFinish();
}
