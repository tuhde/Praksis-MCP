# Praksis MCP

An [MCP](https://modelcontextprotocol.io) server for the [Praksis](https://github.com/tuhde/Praksis)
daemon, in Java. An agent (Claude Code, Claude Desktop, anything that speaks MCP) gets tools to look
at the daemon and, later, to propose changes. **It has no rights of its own:** the server is a client
of the daemon's web API like [Praksis-Web](https://github.com/tuhde/Praksis-Web), so it can do only
what the token you give it may do. Approvals, grants, pins, plan runs and layer updates stay with
you at the daemon, and no tool offers them.

**Status:** skeleton. One tool (`praksis_status`); the read tools, `verify` and the proposal tools
follow (see `docs/PLAN.md` of Praksis, "MCP server").

## Try it

1. Turn on the web listener of the daemon (`[web] listen = "127.0.0.1:8484"` in `praksis.toml`) and
   restart it.
2. `praksis web token create claude --read-only` (shown once).
3. Build (the core must be installed first: `./mvnw install` in `../Praksis`):

       ./mvnw package

   and add the server to your client, for Claude Code:

       claude mcp add praksis --env PRAKSIS_TOKEN=pwt_... -- java -jar /path/to/Praksis-MCP/target/praksis-mcp.jar

## Settings (environment)

| Variable | Meaning |
|---|---|
| `PRAKSIS_TOKEN` | the web token (required; not accepted as an argument, so it is not in a process list) |
| `PRAKSIS_URL` | the address of the listener, default `http://127.0.0.1:8484` |
| `PRAKSIS_MODE` | `read-only` (default) offers only tools that look; `commands` also offers tools that start commands. The daemon enforces the token whichever you choose, so this only decides what the client sees. |

One server per daemon, so one per identity. The log goes to standard error; standard output is the
protocol.

## Tests

`./mvnw verify` runs the tests: the configuration, the client of the daemon against a fake listener,
the tools by mode (and that no tool is named like an approval or a grant), and the built program in
its own process spoken to by a real MCP client.

## License

Apache-2.0.
