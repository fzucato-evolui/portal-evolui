//go:build windows

package sysinfo

import (
	"net"
	"strings"

	"golang.org/x/sys/windows/registry"
)

// dnsFromRegistryWindows lê NameServer / DhcpNameServer em
// HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Interfaces\* (fallback quando WMI não traz DNSServerSearchOrder).
func dnsFromRegistryWindows() []string {
	seen := map[string]struct{}{}
	var out []string
	k, err := registry.OpenKey(registry.LOCAL_MACHINE, `SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Interfaces`, registry.ENUMERATE_SUB_KEYS|registry.READ)
	if err != nil {
		return nil
	}
	defer k.Close()
	names, err := k.ReadSubKeyNames(0)
	if err != nil {
		return nil
	}
	for _, name := range names {
		sk, err := registry.OpenKey(k, name, registry.READ)
		if err != nil {
			continue
		}
		for _, valName := range []string{"NameServer", "DhcpNameServer"} {
			s, _, err := sk.GetStringValue(valName)
			if err != nil {
				continue
			}
			for _, part := range strings.FieldsFunc(s, func(r rune) bool {
				return r == ',' || r == ' ' || r == ';' || r == '\t'
			}) {
				p := strings.TrimSpace(part)
				if p == "" || net.ParseIP(p) == nil {
					continue
				}
				if _, ok := seen[p]; ok {
					continue
				}
				seen[p] = struct{}{}
				out = append(out, p)
			}
		}
		sk.Close()
	}
	return out
}

// defaultGatewaysFromRegistry lê DefaultGateway / DhcpDefaultGateway por interface (fallback quando WMI não preenche gateway).
func defaultGatewaysFromRegistry() (gw4, gw6 string) {
	k, err := registry.OpenKey(registry.LOCAL_MACHINE, `SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Interfaces`, registry.ENUMERATE_SUB_KEYS|registry.READ)
	if err != nil {
		return "", ""
	}
	defer k.Close()
	names, err := k.ReadSubKeyNames(0)
	if err != nil {
		return "", ""
	}
	for _, name := range names {
		sk, err := registry.OpenKey(k, name, registry.READ)
		if err != nil {
			continue
		}
		for _, valName := range []string{"DefaultGateway", "DhcpDefaultGateway"} {
			s, _, err := sk.GetStringValue(valName)
			if err != nil {
				continue
			}
			for _, g := range strings.FieldsFunc(strings.TrimSpace(s), func(r rune) bool {
				return r == ',' || r == ' '
			}) {
				g = strings.TrimSpace(g)
				if g == "" {
					continue
				}
				if strings.Contains(g, ":") {
					if gw6 == "" {
						gw6 = g
					}
				} else {
					if gw4 == "" {
						gw4 = g
					}
				}
			}
		}
		sk.Close()
	}
	return gw4, gw6
}

// fqdnFromRegistryTcpip monta host DNS a partir de NV Hostname + Domain em Tcpip\Parameters (quando WMI não traz DNSDomain).
func fqdnFromRegistryTcpip() string {
	k, err := registry.OpenKey(registry.LOCAL_MACHINE, `SYSTEM\CurrentControlSet\Services\Tcpip\Parameters`, registry.READ)
	if err != nil {
		return ""
	}
	defer k.Close()
	host, _, err := k.GetStringValue("Hostname")
	if err != nil {
		host, _, err = k.GetStringValue("NV Hostname")
	}
	domain, _, derr := k.GetStringValue("Domain")
	if derr != nil {
		return ""
	}
	host = strings.TrimSpace(host)
	domain = strings.TrimSpace(domain)
	if host == "" || domain == "" {
		return ""
	}
	if strings.EqualFold(domain, "WORKGROUP") {
		return ""
	}
	if strings.Contains(domain, ".") {
		return host + "." + domain
	}
	return domain
}
