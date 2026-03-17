package fr.univ_lille.iut_info.steps

interface ExecutionStep {
    fun run(): List<String>
}