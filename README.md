# Praksis MCP

An [MCP](https://modelcontextprotocol.io) server for the [Praksis](https://github.com/tuhde/Praksis)
daemon, in Java. An agent (Claude Code, Claude Desktop, anything that speaks MCP) gets tools to look
at the daemon and, later, to propose changes. **It has no rights of its own:** the server is a client
of the daemon's web API like [Praksis-Web](https://github.com/tuhde/Praksis-Web), so it can do only
what the token you give it may do. Approvals, grants, pins, plan runs and layer updates stay with
you at the daemon, and no tool offers them.

**Status:** the read tools and the resources. Tools: `praksis_status`, `praksis_effective`,
`praksis_layers`, `praksis_rules_list`, `praksis_rule_read`, `praksis_history_find`,
`praksis_history_trace`, `praksis_history_summary`, `praksis_log`, `praksis_audit`,
`praksis_approvals_list`, `praksis_proposals_list`, `praksis_proposal_show`, `praksis_plans_list`,
`praksis_plan_show`, `praksis_help` and `praksis_verify` (a candidate ruleset, never with the option
that fetches). Resources: `praksis://help` (the pages), `praksis://help/<page>` and
`praksis://help/<section>/<page>` (a page in Markdown) and `praksis://effective`. The proposal
tools, the wrapper for text from outside and the limits follow (see `docs/PLAN.md` of Praksis,
"MCP server").

The daemon needs the requests `inspect` and `verify` for web tokens, so build it from a commit that
has them (the core at `ba9bd75` or later). The server waits when it has four requests under way,
because the daemon answers a ninth request at once of one token with 429.

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
