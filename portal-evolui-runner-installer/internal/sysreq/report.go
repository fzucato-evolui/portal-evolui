package sysreq

// Report descreve se o host atende requisitos do pacote oficial actions/runner (runtime Node em externals/node*, versão por release).
type Report struct {
	Meets  bool
	Detail string
}
