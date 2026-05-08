package sysreq

// Report descreve se o host atende requisitos do pacote oficial actions/runner (runtime Node em externals/node*, versão por release).
type Report struct {
	Meets    bool
	Detail   string
	Elevated bool // processo rodando como Administrator (Windows) / root (Linux); usado pela UI para validar instalação como serviço.
}
